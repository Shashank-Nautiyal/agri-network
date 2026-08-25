package com.nullpointer.agri_backend.controller;


import com.nullpointer.agri_backend.dto.AdvisoryRequest;
import com.nullpointer.agri_backend.dto.AdvisoryResponse;
import com.nullpointer.agri_backend.service.AdvisoryHistoryService;
import com.nullpointer.agri_backend.service.AiServiceClient;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/advisory")
public class AdvisoryController {

    private final AiServiceClient aiServiceClient;
    private final AdvisoryHistoryService historyService;

    public AdvisoryController(AiServiceClient aiServiceClient, AdvisoryHistoryService historyService) {
        this.aiServiceClient = aiServiceClient;
        this.historyService = historyService;
    }

    @PostMapping
    public AdvisoryResponse advisory(@Valid @RequestBody AdvisoryRequest request) {
        AdvisoryResponse response = aiServiceClient.advisory(request);

        historyService.recordAdvisory(request.getFarmerId(), request.getDistrictId(), response.getRecommendation());

        return response;
    }
}
