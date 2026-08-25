package com.nullpointer.agri_backend.controller;


import com.nullpointer.agri_backend.model.Farmer;
import com.nullpointer.agri_backend.service.FarmerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/farmers")
public class FarmerController {

    private final FarmerService farmerService;

    public FarmerController(FarmerService farmerService) {
        this.farmerService = farmerService;
    }

    @PostMapping
    public Farmer register(@RequestBody Farmer farmer) {
        return farmerService.create(farmer);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Farmer> get(@PathVariable String id) {
        return farmerService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
