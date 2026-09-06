package com.nullpointer.agri_backend.controller;

import com.nullpointer.agri_backend.dto.RegenerativeRequest;
import com.nullpointer.agri_backend.dto.RegenerativeResponse;
import com.nullpointer.agri_backend.service.AiServiceClient;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/regenerative-advice")
public class RegenerativeAdviceController {

    private final AiServiceClient aiServiceClient;

    public RegenerativeAdviceController(AiServiceClient aiServiceClient) {
        this.aiServiceClient = aiServiceClient;
    }

    @PostMapping
    public RegenerativeResponse regenerativeAdvice(@Valid @RequestBody RegenerativeRequest request) {
        return aiServiceClient.regenerativeAdvice(request);
    }
}
