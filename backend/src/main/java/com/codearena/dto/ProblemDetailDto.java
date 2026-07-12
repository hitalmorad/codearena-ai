package com.codearena.dto;

import com.codearena.model.Difficulty;
import com.codearena.model.Language;
import java.util.List;
import java.util.Map;

public record ProblemDetailDto(
        Long id,
        String slug,
        String title,
        String description,
        Difficulty difficulty,
        List<String> tags,
        Map<Language, String> starterCode,
        int timeLimitMs,
        int memoryLimitMb,
        List<SampleTestCaseDto> sampleTestCases
) {
    public record SampleTestCaseDto(String input, String expectedOutput) {
    }
}
