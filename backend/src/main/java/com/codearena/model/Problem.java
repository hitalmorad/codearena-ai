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
import jakarta.persistence.Lob;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "problems")
@Getter
@Setter
@NoArgsConstructor
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty = Difficulty.EASY;

    /** Comma-free list of topic tags, e.g. "arrays", "two-pointers". */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "problem_tags", joinColumns = @JoinColumn(name = "problem_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    /** Per-language starter code shown in the editor. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "problem_starter_code", joinColumns = @JoinColumn(name = "problem_id"))
    @MapKeyColumn(name = "language")
    @MapKeyEnumerated(EnumType.STRING)
    @Lob
    @Column(name = "code", columnDefinition = "TEXT")
    private Map<Language, String> starterCode = new HashMap<>();

    @Column(nullable = false)
    private int timeLimitMs = 2000;

    @Column(nullable = false)
    private int memoryLimitMb = 256;

    @OneToMany(mappedBy = "problem", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<TestCase> testCases = new ArrayList<>();

    public void addTestCase(TestCase testCase) {
        testCase.setProblem(this);
        this.testCases.add(testCase);
    }
}
