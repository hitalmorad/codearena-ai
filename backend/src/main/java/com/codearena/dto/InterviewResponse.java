package com.codearena.dto;

import java.util.List;

public record InterviewResponse(
        String reply,
        List<ChatMessage> history,
        String source
) {
}
