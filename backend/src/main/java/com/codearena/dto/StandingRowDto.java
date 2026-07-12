package com.codearena.dto;

import java.util.List;

public record StandingRowDto(
        int rank,
        String username,
        int rating,
        int solvedCount,
        long penaltyMinutes,
        Integer ratingDelta,
        List<String> solvedSlugs
) {
}
