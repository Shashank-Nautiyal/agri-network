package com.nullpointer.agri_backend.service;


import com.nullpointer.agri_backend.dto.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Boundary between our Spring Boot backend and the teammate's AI/ML service
 * (Gemini multimodal + Earth Engine), per the agreed API contract:
 *
 *   POST /diagnose      -> DiagnoseResponse
 *   POST /advisory       -> AdvisoryResponse
 *   POST /voice-query    -> VoiceQueryResponse
 *
 * PHASE 1: returns mocked responses so the end-to-end flow works and can be
 *          deployed immediately, without waiting on the AI service.
 * PHASE 2: flip USE_MOCK to false (or just delete the mock branch) once the
 *          teammate's service is live at ai.service.base-url.
 */
@Service
public class AiServiceClient {

    private static final boolean USE_MOCK = true;

    private final WebClient webClient;

    public AiServiceClient(@Value("${ai.service.base-url:http://localhost:8000}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public DiagnoseResponse diagnose(DiagnoseRequest request) {
        if (USE_MOCK) {
            return new DiagnoseResponse(
                    "Early Blight (mock)",
                    0.87,
                    "Mock advice: remove affected leaves, apply neem-based spray, avoid overhead watering.",
                    "en"
            );
        }
        return webClient.post()
                .uri("/diagnose")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DiagnoseResponse.class)
                .block();
    }

    public AdvisoryResponse advisory(@Valid AdvisoryRequest request) {
        if (USE_MOCK) {
            return new AdvisoryResponse(
                    "Mock recommendation: soil moisture is moderate; consider drought-tolerant millet this season.",
                    "NDVI: 0.62 (healthy vegetation, mock)",
                    "Low risk of frost in next 7 days (mock)",
                    "en"
            );
        }
        return webClient.post()
                .uri("/advisory")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AdvisoryResponse.class)
                .block();
    }

    public VoiceQueryResponse voiceQuery(VoiceQueryRequest request) {
        if (USE_MOCK) {
            String transcript = request.getTranscript() != null
                    ? request.getTranscript()
                    : "(mock transcript) What should I plant this season?";
            return new VoiceQueryResponse(
                    transcript,
                    "Mock response: based on your district's soil and rainfall, millet is a good choice this season.",
                    null,
                    request.getLanguageHint() != null ? request.getLanguageHint() : "en"
            );
        }
        return webClient.post()
                .uri("/voice-query")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(VoiceQueryResponse.class)
                .block();
    }
}
