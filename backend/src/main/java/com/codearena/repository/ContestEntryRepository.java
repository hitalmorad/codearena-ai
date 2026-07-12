package com.codearena.repository;

import com.codearena.model.ContestEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContestEntryRepository extends JpaRepository<ContestEntry, Long> {
    List<ContestEntry> findByContestId(Long contestId);

    Optional<ContestEntry> findByContestIdAndUserId(Long contestId, Long userId);

    long countByContestId(Long contestId);

    List<ContestEntry> findByUserId(Long userId);
}
