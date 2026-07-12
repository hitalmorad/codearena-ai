package com.codearena.judge;

import com.codearena.model.Verdict;
import java.util.List;

/**
 * Outcome of judging a submission: overall verdict plus per-test-case details.
 */
public record JudgeResult(
        Verdict verdict,
        int passedTests,
        int totalTests,
        Integer runtimeMs,
        String message,
        List<CaseResult> cases
) {
    public static JudgeResult internalError(String message) {
        return new JudgeResult(Verdict.INTERNAL_ERROR, 0, 0, null, message, List.of());
    }
}
