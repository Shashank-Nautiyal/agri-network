package com.nullpointer.agri_backend.controller;


import com.nullpointer.agri_backend.dto.DiagnoseRequest;
import com.nullpointer.agri_backend.dto.DiagnoseResponse;
import com.nullpointer.agri_backend.service.AdvisoryHistoryService;
import com.nullpointer.agri_backend.service.AiServiceClient;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diagnose")
public class DiagnosisController {

    private final AiServiceClient aiServiceClient;
    private final AdvisoryHistoryService historyService;

    public DiagnosisController(AiServiceClient aiServiceClient, AdvisoryHistoryService historyService) {
        this.aiServiceClient = aiServiceClient;
        this.historyService = historyService;
    }

    @PostMapping
    public DiagnoseResponse diagnose(@Valid @RequestBody DiagnoseRequest request) {
        DiagnoseResponse response = aiServiceClient.diagnose(request);

        // Log to history for farmer's record + anonymized district trend data
        // (Phase 2: wire this up to Firestore in AdvisoryHistoryService)
        if (response.isValidImage()) {
            historyService.recordDiagnosis(request.getFarmerId(), request.getDistrictId(), response.getDisease());
        }

        return response;
    }
}
