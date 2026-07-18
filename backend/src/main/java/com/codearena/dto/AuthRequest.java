package com.codearena.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AuthRequest(
        @NotBlank
        @Size(min = 2, max = 20)
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "may only contain letters, numbers and underscores")
        String username,

        /** Required for registration, ignored for login. */
        @Email(message = "must be a valid email address")
        @Size(max = 120)
        String email,

        @NotBlank
        @Size(min = 4, max = 100)
        String password
) {
}
