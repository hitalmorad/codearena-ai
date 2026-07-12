package com.codearena.dto;

import com.codearena.model.Language;
import com.codearena.model.Submission;
import com.codearena.model.Verdict;
import java.time.Instant;

public record SubmissionResponse(
        Long id,
        Long problemId,
        String problemSlug,
        String problemTitle,
        Language language,
        Verdict verdict,
        Integer runtimeMs,
        Integer memoryKb,
        int passedTests,
        int totalTests,
        String message,
        Instant createdAt
) {
    public static SubmissionResponse from(Submission s) {
        return new SubmissionResponse(
                s.getId(),
                s.getProblem().getId(),
                s.getProblem().getSlug(),
                s.getProblem().getTitle(),
                s.getLanguage(),
                s.getVerdict(),
                s.getRuntimeMs(),
                s.getMemoryKb(),
                s.getPassedTests(),
                s.getTotalTests(),
                s.getMessage(),
                s.getCreatedAt()
        );
    }
}
