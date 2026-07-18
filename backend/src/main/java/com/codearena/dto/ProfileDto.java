package com.codearena.dto;

public record ProfileDto(
        String username,
        String bio,
        String email,
        String role,
        String memberSince,
        long totalContests,
        int rating,
        int rank,
        long totalUsers,
        long solvedEasy,
        long solvedMedium,
        long solvedHard,
        long totalEasy,
        long totalMedium,
        long totalHard,
        long totalSubmissions,
        long acceptedSubmissions
) {
}
