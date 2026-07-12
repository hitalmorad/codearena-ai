package com.codearena.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/** Admin payload to create a contest from existing problems. */
public record AdminContestRequest(
        @NotBlank String name,
        String description,
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        List<String> problemSlugs
) {
}
