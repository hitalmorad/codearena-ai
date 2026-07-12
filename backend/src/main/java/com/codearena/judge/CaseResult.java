package com.codearena.judge;

/**
 * Result of running one test case. Whether input/expected/actual are exposed
 * to the client is decided by the service layer (revealed for practice sample
 * cases, hidden for contest / hidden cases).
 */
public record CaseResult(
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
