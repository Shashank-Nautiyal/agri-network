package com.nullpointer.agri_backend.service;


import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.nullpointer.agri_backend.model.AdvisoryRecord;
import com.nullpointer.agri_backend.model.DiseaseTrendEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@Service
public class AdvisoryHistoryService {

    private static final String COLLECTION = "advisory_records";
    private final Firestore firestore;

    // How many trailing days count toward a district's disease trend, and
    // how many reports of the *same* disease within that window before it's
    // flagged as an alert rather than just background noise. Configurable
    // via application.properties (agri.alerts.window-days / agri.alerts.threshold)
    // so these can be tuned without a redeploy.
    private final int trendWindowDays;
    private final long alertThreshold;

    public AdvisoryHistoryService(
            Firestore firestore,
            @Value("${agri.alerts.window-days:7}") int trendWindowDays,
            @Value("${agri.alerts.threshold:3}") long alertThreshold) {
        this.firestore = firestore;
        this.trendWindowDays = trendWindowDays;
        this.alertThreshold = alertThreshold;
    }

    public void recordDiagnosis(String farmerId, String districtId, String diseaseSummary) {
        save(new AdvisoryRecord(UUID.randomUUID().toString(), farmerId, districtId, "diagnosis", normalizeDiseaseName(diseaseSummary), Timestamp.now()));
    }

    public void recordAdvisory(String farmerId, String districtId, String recommendationSummary) {
        save(new AdvisoryRecord(UUID.randomUUID().toString(), farmerId, districtId, "advisory", recommendationSummary, Timestamp.now()));
    }

    /**
     * Collapses trivial formatting differences ("leaf blight", "LEAF BLIGHT",
     * "  Leaf Blight ") down to one consistent key ("Leaf Blight") before the
     * value is stored or grouped.
     *
     * This matters because diseaseTrendByDistrict() groups reports by an
     * EXACT string match on this field. The AI service's prompt has been
     * tightened to request a short, consistent disease name, but a language
     * model's output still isn't guaranteed to be byte-identical across
     * calls -- and older records already in Firestore were written before
     * that prompt change existed. Without this, two farmers reporting the
     * same real disease could silently never be counted together, and the
     * alert threshold could fail to trigger even during a genuine outbreak.
     *
     * This only fixes whitespace/casing variance, not deeper phrasing
     * differences (e.g. "Leaf Blight" vs "Early Blight" for the same
     * underlying disease) -- that ceiling is a prompt/data problem, not
     * something string normalization alone can solve.
     */
    private static String normalizeDiseaseName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Unknown";
        }
        String[] words = raw.trim().replaceAll("\\s+", " ").toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            result.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1))
                  .append(" ");
        }
        return result.toString().trim();
    }

    private void save(AdvisoryRecord record) {
        try {
            firestore.collection(COLLECTION).document(record.getId()).set(record).get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to save advisory record", e);
        }
    }

    public List<AdvisoryRecord> historyForFarmer(String farmerId) {
        try {
            QuerySnapshot snapshot = firestore.collection(COLLECTION)
                    .whereEqualTo("farmerId", farmerId)
                    .get().get();

            List<AdvisoryRecord> result = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
                result.add(doc.toObject(AdvisoryRecord.class));
            }
            return result;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to fetch farmer history", e);
        }
    }

    /**
     * Anonymized cross-district trend data within the trailing window
     * (default 7 days) - e.g. "3 blight reports in District X this week,
     * flagged as an alert because that's >= the configured threshold".
     * This is what powers the regional-alerts / cross-state
     * interoperability endpoint.
     *
     * Previously this counted every diagnosis record ever stored for the
     * district with no time bound at all, so the number only ever grew and
     * couldn't distinguish "an outbreak this week" from "this disease has
     * been seen here at some point since launch." Windowing it, and adding
     * an explicit alert flag, is what makes this an actual early-warning
     * signal instead of a running total.
     */
    public Map<String, DiseaseTrendEntry> diseaseTrendByDistrict(String districtId) {
        try {
            Timestamp windowStart = Timestamp.of(
                    Date.from(Instant.now().minus(trendWindowDays, ChronoUnit.DAYS)));

            // NOTE: this combines two equality filters (districtId, type)
            // with a range filter (timestamp) on a different field, which
            // Firestore requires a composite index for. If this hasn't
            // been created yet, the query fails at runtime with
            // FAILED_PRECONDITION and a console link to auto-create it —
            // see firestore.indexes.json added alongside this change, and
            // create the index BEFORE demo day, not during it.
            QuerySnapshot snapshot = firestore.collection(COLLECTION)
                    .whereEqualTo("districtId", districtId)
                    .whereEqualTo("type", "diagnosis")
                    .whereGreaterThanOrEqualTo("timestamp", windowStart)
                    .get().get();

            Map<String, Long> counts = new ConcurrentHashMap<>();
            for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
                AdvisoryRecord record = doc.toObject(AdvisoryRecord.class);
                counts.merge(normalizeDiseaseName(record.getSummary()), 1L, Long::sum);
            }

            Map<String, DiseaseTrendEntry> trend = new LinkedHashMap<>();
            for (Map.Entry<String, Long> entry : counts.entrySet()) {
                long count = entry.getValue();
                trend.put(entry.getKey(), new DiseaseTrendEntry(count, count >= alertThreshold, trendWindowDays));
            }
            return trend;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to fetch regional alerts", e);
        }
    }
}
