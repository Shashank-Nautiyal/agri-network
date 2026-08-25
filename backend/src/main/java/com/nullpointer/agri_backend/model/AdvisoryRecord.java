package com.nullpointer.agri_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * A single diagnosis or advisory event, stored per-farmer and also
 * aggregated (anonymized) at district level for the regional-alerts
 * / cross-state interoperability feature.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdvisoryRecord {
    private String id;
    private String farmerId;
    private String districtId;
    private String type;       // "diagnosis" or "advisory"
    private String summary;    // e.g. disease name or recommendation headline
    private Instant timestamp;
}
