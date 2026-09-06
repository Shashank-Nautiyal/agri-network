package com.nullpointer.agri_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Mirrors the AI service's Location schema. district/state/country come
 * from a manual dropdown on the frontend (not reverse geocoding, to avoid
 * a billed geocoding API); latitude/longitude come from the browser's
 * Geolocation API.
 */
@Data
public class Location {
    @NotBlank
    private String country;

    @NotBlank
    private String state;

    @NotBlank
    private String district;

    private double latitude;
    private double longitude;
}
