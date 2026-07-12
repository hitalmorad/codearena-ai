package com.codearena.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A time-boxed contest containing an ordered set of problems.
 */
@Entity
@Table(name = "contests")
@Getter
@Setter
@NoArgsConstructor
public class Contest {

    public enum Status { UPCOMING, RUNNING, ENDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Instant startTime;

    @Column(nullable = false)
    private Instant endTime;

    @ManyToMany
    @JoinTable(
            name = "contest_problems",
            joinColumns = @JoinColumn(name = "contest_id"),
            inverseJoinColumns = @JoinColumn(name = "problem_id"))
    @OrderColumn(name = "position")
    private List<Problem> problems = new ArrayList<>();

    /** Whether Elo rating changes have been applied after the contest ended. */
    @Column(nullable = false)
    private boolean ratingApplied = false;

    public Status statusAt(Instant now) {
        if (now.isBefore(startTime)) {
            return Status.UPCOMING;
        }
        if (now.isAfter(endTime)) {
            return Status.ENDED;
        }
        return Status.RUNNING;
    }
}
