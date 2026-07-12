package com.codearena.dto;

import jakarta.validation.constraints.Size;

public record UpdateBioRequest(
        @Size(max = 280) String bio
) {
}
