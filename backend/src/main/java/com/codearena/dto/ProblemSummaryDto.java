package com.codearena.dto;

import com.codearena.model.Difficulty;
import java.util.List;

public record ProblemSummaryDto(
        Long id,
        String slug,
        String title,
        Difficulty difficulty,
        List<String> tags
) {
}
