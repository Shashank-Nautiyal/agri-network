package com.nullpointer.agri_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Farmer {
    private String id;
    private String name;
    private String phone;
    private String districtId;
    private String preferredLanguage; // e.g. "hi", "en"
}
