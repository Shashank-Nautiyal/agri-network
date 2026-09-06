package com.nullpointer.agri_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdvisoryResponse {
    private String recommendation;
    private String ndviSummary;
    private String soilSummary;
    private String weatherRisk;
    private String diseaseRiskLevel;
    private double confidence;
    private String language;
}
