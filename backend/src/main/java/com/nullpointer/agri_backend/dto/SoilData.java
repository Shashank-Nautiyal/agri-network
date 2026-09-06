package com.nullpointer.agri_backend.dto;

import lombok.Data;

@Data
public class SoilData {
    private double ph;
    private double nitrogen;
    private double phosphorus;
    private double potassium;
    private double organicCarbon;
    private double moisture;
}
