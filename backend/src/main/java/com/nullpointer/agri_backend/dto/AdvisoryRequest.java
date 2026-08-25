package com.nullpointer.agri_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdvisoryRequest {
    @NotBlank
    private String districtId;

    private String cropType; // optional
    private String farmerId; // optional for now
}
