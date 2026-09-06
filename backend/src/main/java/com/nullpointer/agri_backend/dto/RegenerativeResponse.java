package com.nullpointer.agri_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegenerativeResponse {
    private List<String> practices;
    private String reasoning;
    private String expectedBenefit;
}
