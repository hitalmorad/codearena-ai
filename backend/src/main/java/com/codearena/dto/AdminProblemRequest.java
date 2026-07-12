package com.codearena.dto;

import com.codearena.model.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Admin payload to create or update a problem with its test cases. */
public record AdminProblemRequest(
        @NotBlank String slug,
        @NotBlank String title,
        @NotNull Difficulty difficulty,
        @NotBlank String description,
        List<String> tags,
        Integer timeLimitMs,
        Integer memoryLimitMb,
        List<TestCaseInput> testcases
) {
    public record TestCaseInput(
            String input,
            String expectedOutput,
            boolean sample
    ) {
    }
}
