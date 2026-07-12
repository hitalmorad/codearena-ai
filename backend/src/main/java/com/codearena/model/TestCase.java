package com.codearena.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "test_cases")
@Getter
@Setter
@NoArgsConstructor
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String input;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String expectedOutput;

    /** Sample cases are shown to the user; hidden cases are used only for judging. */
    @Column(nullable = false)
    private boolean sample = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id")
    private Problem problem;

    public TestCase(String input, String expectedOutput, boolean sample) {
        this.input = input;
        this.expectedOutput = expectedOutput;
        this.sample = sample;
    }
}
