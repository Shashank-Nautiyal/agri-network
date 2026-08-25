package com.nullpointer.agri_backend.dto;

import lombok.Data;

@Data
public class VoiceQueryRequest {
    // NOTE: if using the browser Web Speech API on the frontend, STT/TTS happens
    // client-side, so this endpoint may just receive a plain transcript instead
    // of raw audio. Keeping both fields optional so either flow works.
    private String audioBase64;
    private String transcript;
    private String farmerId;
    private String languageHint; // e.g. "hi-IN", "en-IN"
}
