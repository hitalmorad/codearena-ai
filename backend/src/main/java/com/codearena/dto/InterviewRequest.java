package com.codearena.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record InterviewRequest(
        String slug,
        List<ChatMessage> history,
        @NotBlank String message
) {
}
