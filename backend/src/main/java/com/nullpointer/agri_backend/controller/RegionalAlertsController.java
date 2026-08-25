package com.nullpointer.agri_backend.controller;


import com.nullpointer.agri_backend.service.AdvisoryHistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Demonstrates the "digital public good / scales across states" requirement:
 * any state/district can query anonymized, aggregated disease trend data to
 * spot emerging outbreaks - this is the visible proof of the interoperability
 * story for the pitch deck and demo, not just a claim.
 */
@RestController
@RequestMapping("/api/district")
public class RegionalAlertsController {

    private final AdvisoryHistoryService historyService;

    public RegionalAlertsController(AdvisoryHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/{districtId}/regional-alerts")
    public Map<String, Long> regionalAlerts(@PathVariable String districtId) {
        return historyService.diseaseTrendByDistrict(districtId);
    }
}
