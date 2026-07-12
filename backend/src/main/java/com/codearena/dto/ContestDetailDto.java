package com.codearena.dto;

import com.codearena.model.Contest;
import com.codearena.model.Difficulty;
import java.time.Instant;
import java.util.List;

public record ContestDetailDto(
        Long id,
        String name,
        String description,
        Contest.Status status,
        Instant startTime,
        Instant endTime,
        long participantCount,
        boolean registered,
        List<ProblemRef> problems
) {
    public record ProblemRef(String slug, String title, Difficulty difficulty) {
    }
}
