package com.codearena.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

/**
 * A competitor. Ratings start at 1200 and change as problems are solved and
 * contests are played.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    public static final int STARTING_RATING = 1200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    /** Email address; used to grant admin access via the configured allowlist. */
    @Column(unique = true)
    private String email;

    /** PBKDF2 hash ("salt:hash"). Null for display-only demo accounts. */
    @Column
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    /** Opaque session token; rotated on each login. */
    @Column(unique = true)
    private String token;

    /** Bumped whenever all existing JWTs should be invalidated (e.g. password change). */
    @Column(nullable = false)
    @ColumnDefault("0")
    private int tokenVersion = 0;

    @Column(nullable = false)
    private int rating = STARTING_RATING;

    @Column(nullable = false)
    private int problemsSolved = 0;

    /** Hidden weighted problem-solving score, used only for global ranking. */
    @Column(nullable = false)
    @ColumnDefault("0")
    private int score = 0;

    /** IDs of problems this user has solved at least once (global, all-time). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_solved_problems", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "problem_id")
    private Set<Long> solvedProblemIds = new HashSet<>();

    /** Short public bio shown on the profile page. */
    @Column(length = 280)
    private String bio;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public User(String username) {
        this.username = username;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
