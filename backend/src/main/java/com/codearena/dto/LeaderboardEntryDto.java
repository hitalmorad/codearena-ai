package com.codearena.dto;

public record LeaderboardEntryDto(
        int rank,
        String username,
        int rating,
        int problemsSolved
) {
}
