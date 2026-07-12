package com.codearena.dto;

/** A single chat turn. Role is "user" or "assistant". */
public record ChatMessage(
        String role,
        String content
) {
}
