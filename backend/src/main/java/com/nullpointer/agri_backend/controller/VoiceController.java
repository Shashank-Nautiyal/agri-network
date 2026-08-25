package com.nullpointer.agri_backend.controller;


import com.nullpointer.agri_backend.dto.VoiceQueryRequest;
import com.nullpointer.agri_backend.dto.VoiceQueryResponse;
import com.nullpointer.agri_backend.service.AiServiceClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/voice-query")
public class VoiceController {

    private final AiServiceClient aiServiceClient;

    public VoiceController(AiServiceClient aiServiceClient) {
        this.aiServiceClient = aiServiceClient;
    }

    @PostMapping
    public VoiceQueryResponse voiceQuery(@RequestBody VoiceQueryRequest request) {
        // If the frontend uses the browser Web Speech API, `transcript` will
        // already be filled in and `audioBase64` can be left null - no server-side
        // STT needed. This keeps us off any billed Speech-to-Text API.
        return aiServiceClient.voiceQuery(request);
    }
}
