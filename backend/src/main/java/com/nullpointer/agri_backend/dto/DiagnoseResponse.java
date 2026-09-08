package com.nullpointer.agri_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiagnoseResponse {
    private String disease;
    private double confidence;
    private String treatmentAdvice;
    private String language;
    private boolean isValidImage = true;
}
