package com.nullpointer.agri_backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegenerativeRequest {
    @NotBlank
    private String farmerId;

    @NotNull
    @Valid
    private Location location;

    @Valid
    private SoilData soilData;

    @NotBlank
    private String cropType;
}
