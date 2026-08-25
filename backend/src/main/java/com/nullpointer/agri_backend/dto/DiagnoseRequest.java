package com.nullpointer.agri_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DiagnoseRequest {
    @NotBlank
    private String imageBase64;

    @NotBlank
    private String districtId;

    private String farmerId; // optional for now, required once auth/profiles are wired in
}
