package com.nullpointer.agri_backend.service;


import com.nullpointer.agri_backend.model.AdvisoryRecord;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PHASE 1: in-memory storage so the flow works end-to-end immediately.
 * PHASE 2: swap the in-memory maps for Firestore reads/writes (see
 *          FirestoreConfig) - the method signatures below shouldn't need to
 *          change, only the implementation.
 */
@Service
public class AdvisoryHistoryService {

    private final List<AdvisoryRecord> records = Collections.synchronizedList(new ArrayList<>());

    public void recordDiagnosis(String farmerId, String districtId, String diseaseSummary) {
        records.add(new AdvisoryRecord(
                UUID.randomUUID().toString(), farmerId, districtId, "diagnosis", diseaseSummary, Instant.now()));
    }

    public void recordAdvisory(String farmerId, String districtId, String recommendationSummary) {
        records.add(new AdvisoryRecord(
                UUID.randomUUID().toString(), farmerId, districtId, "advisory", recommendationSummary, Instant.now()));
    }

    public List<AdvisoryRecord> historyForFarmer(String farmerId) {
        return records.stream().filter(r -> r.getFarmerId() != null && r.getFarmerId().equals(farmerId)).toList();
    }

    /**
     * Anonymized cross-district trend data - e.g. "3 blight reports in
     * District X this week". This is what powers the regional-alerts /
     * cross-state interoperability endpoint, demonstrating the "digital
     * public good, scales across states" requirement.
     */
    public Map<String, Long> diseaseTrendByDistrict(String districtId) {
        Map<String, Long> trend = new ConcurrentHashMap<>();
        records.stream()
                .filter(r -> "diagnosis".equals(r.getType()) && districtId.equals(r.getDistrictId()))
                .forEach(r -> trend.merge(r.getSummary(), 1L, Long::sum));
        return trend;
    }
}
