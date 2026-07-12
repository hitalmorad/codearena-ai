package com.codearena.repository;

import com.codearena.model.Difficulty;
import com.codearena.model.Problem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
    Optional<Problem> findBySlug(String slug);

    boolean existsBySlug(String slug);

    long countByDifficulty(Difficulty difficulty);
}
