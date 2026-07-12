package com.codearena.dto;

import com.codearena.model.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmissionRequest(
        @NotNull Language language,
        @NotBlank @Size(max = 50_000) String sourceCode,
        String username
) {
}
