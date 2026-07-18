package com.codearena.repository;

import com.codearena.model.Role;
import com.codearena.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByToken(String token);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    long countByRatingGreaterThan(int rating);

    long countByScoreGreaterThan(int score);

    List<User> findTop100ByOrderByRatingDescProblemsSolvedDescUsernameAsc();

    List<User> findTop100ByOrderByScoreDescProblemsSolvedDescUsernameAsc();

    List<User> findTop100ByRoleOrderByScoreDescProblemsSolvedDescUsernameAsc(Role role);
}
