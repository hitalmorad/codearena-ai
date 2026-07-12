package com.codearena.repository;

import com.codearena.model.Contest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContestRepository extends JpaRepository<Contest, Long> {
    List<Contest> findAllByOrderByStartTimeDesc();
}
