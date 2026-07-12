package com.codearena.dto;

import java.util.List;

/**
 * Unified result for both "Run" (sample cases) and "Submit" (all cases).
 * Per-case input/expected/actual are only populated when allowed to be shown
 * (sample cases, or the failing case in practice mode). In contest mode they
 * are redacted (null).
 */
public record JudgeReportDto(
        String verdict,
        int passedTests,
        int totalTests,
        Integer runtimeMs,
        String message,
        boolean contestMode,
        List<CaseView> cases
) {
    public record CaseView(
            int index,
            boolean sample,
            boolean passed,
            String verdict,
            String input,
            String expectedOutput,
            String actualOutput,
            Integer runtimeMs
    ) {
    }
}
