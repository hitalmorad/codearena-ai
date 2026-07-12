package com.codearena.dto;

/** source is "groq" (AI) or "heuristic" (offline fallback). */
public record HintResponse(
        int level,
        String text,
        String source
) {
}
