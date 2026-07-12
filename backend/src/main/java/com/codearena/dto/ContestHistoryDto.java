package com.codearena.dto;

import com.codearena.model.Contest;
import java.time.Instant;

public record ContestHistoryDto(
        Long contestId,
        String name,
        Contest.Status status,
        Instant startTime,
        Instant endTime,
        int solvedCount,
        long penaltyMinutes,
        Integer ratingBefore,
        Integer ratingAfter,
        Integer ratingDelta
) {
}
