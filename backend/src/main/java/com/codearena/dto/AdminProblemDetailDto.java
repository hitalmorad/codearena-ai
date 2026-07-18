package com.codearena.dto;

import com.codearena.model.Difficulty;
import java.util.List;

/** Full problem view for the admin editor, including hidden test cases. */
public record AdminProblemDetailDto(
        String slug,
        String title,
        Difficulty difficulty,
        String description,
        List<String> tags,
        int timeLimitMs,
        int memoryLimitMb,
        List<CaseDto> testcases
) {
    public record CaseDto(String input, String expectedOutput, boolean sample) {
    }
}
