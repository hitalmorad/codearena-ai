package com.codearena.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AuthRequest(
        @NotBlank
        @Size(min = 2, max = 20)
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "may only contain letters, numbers and underscores")
        String username,

        @NotBlank
        @Size(min = 4, max = 100)
        String password
) {
}
