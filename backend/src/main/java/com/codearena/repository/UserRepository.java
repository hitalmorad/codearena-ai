package com.codearena.repository;

import com.codearena.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByToken(String token);

    boolean existsByUsername(String username);

    long countByRatingGreaterThan(int rating);

    List<User> findTop100ByOrderByRatingDescProblemsSolvedDescUsernameAsc();
}
