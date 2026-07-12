package com.codearena.dto;

import com.codearena.model.Contest;
import java.time.Instant;

public record ContestSummaryDto(
        Long id,
        String name,
        Contest.Status status,
        Instant startTime,
        Instant endTime,
        int problemCount,
        long participantCount
) {
}
