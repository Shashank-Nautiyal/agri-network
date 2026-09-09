package com.nullpointer.agri_backend.model;

import com.google.cloud.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


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
    private Timestamp timestamp;
}
