package com.nullpointer.agri_backend.service;


import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.nullpointer.agri_backend.model.AdvisoryRecord;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@Service
public class AdvisoryHistoryService {

    private static final String COLLECTION = "advisory_records";
    private final Firestore firestore;

    public AdvisoryHistoryService(Firestore firestore) {
        this.firestore = firestore;
    }

    public void recordDiagnosis(String farmerId, String districtId, String diseaseSummary) {
        save(new AdvisoryRecord(UUID.randomUUID().toString(), farmerId, districtId, "diagnosis", diseaseSummary, Timestamp.now()));
    }

    public void recordAdvisory(String farmerId, String districtId, String recommendationSummary) {
        save(new AdvisoryRecord(UUID.randomUUID().toString(), farmerId, districtId, "advisory", recommendationSummary, Timestamp.now()));
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
     * Anonymized cross-district trend data - e.g. "3 blight reports in
     * District X this week". This is what powers the regional-alerts /
     * cross-state interoperability endpoint.
     */
    public Map<String, Long> diseaseTrendByDistrict(String districtId) {
        try {
            QuerySnapshot snapshot = firestore.collection(COLLECTION)
                    .whereEqualTo("districtId", districtId)
                    .whereEqualTo("type", "diagnosis")
                    .get().get();

            Map<String, Long> trend = new ConcurrentHashMap<>();
            for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
                AdvisoryRecord record = doc.toObject(AdvisoryRecord.class);
                trend.merge(record.getSummary(), 1L, Long::sum);
            }
            return trend;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to fetch regional alerts", e);
        }
    }
}
