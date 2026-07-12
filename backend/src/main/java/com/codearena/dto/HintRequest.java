package com.codearena.dto;

import jakarta.validation.constraints.NotBlank;

public record HintRequest(
        @NotBlank String slug,
        int level,
        String language,
        String sourceCode
) {
}
