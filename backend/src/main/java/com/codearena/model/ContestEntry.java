package com.codearena.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A user's participation in a contest: their solved problems (within the
 * contest window), penalty time, and rating change once the contest ends.
 */
@Entity
@Table(name = "contest_entries", uniqueConstraints =
        @UniqueConstraint(columnNames = {"contest_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ContestEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contest_id")
    private Contest contest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    /** Problem IDs solved during this contest. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "contest_entry_solved", joinColumns = @JoinColumn(name = "entry_id"))
    @Column(name = "problem_id")
    private Set<Long> solvedProblemIds = new HashSet<>();

    /** ICPC-style penalty: minutes from contest start to each accepted solve. */
    @Column(nullable = false)
    private long penaltyMinutes = 0;

    private Integer ratingBefore;
    private Integer ratingAfter;

    public ContestEntry(Contest contest, User user) {
        this.contest = contest;
        this.user = user;
    }

    public int solvedCount() {
        return solvedProblemIds.size();
    }
}
