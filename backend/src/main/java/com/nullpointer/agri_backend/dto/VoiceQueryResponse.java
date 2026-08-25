package com.nullpointer.agri_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoiceQueryResponse {
    private String transcript;
    private String responseText;
    private String responseAudioBase64; // null if using client-side Web Speech API for TTS
    private String language;
}
