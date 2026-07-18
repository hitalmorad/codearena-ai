package com.codearena.repository;

import com.codearena.model.Submission;
import com.codearena.model.Verdict;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findTop20ByProblemIdOrderByCreatedAtDesc(Long problemId);

    List<Submission> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);

    List<Submission> findByUserIdAndCreatedAtAfter(Long userId, Instant after);

    long countByUserId(Long userId);

    long countByUserIdAndVerdict(Long userId, Verdict verdict);

    void deleteByProblemId(Long problemId);
}
