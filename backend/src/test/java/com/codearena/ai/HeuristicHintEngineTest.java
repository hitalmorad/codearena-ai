package com.codearena.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codearena.model.Difficulty;
import com.codearena.model.Problem;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HeuristicHintEngineTest {

    private final HeuristicHintEngine engine = new HeuristicHintEngine();

    private Problem problem(Difficulty difficulty, List<String> tags) {
        Problem p = new Problem();
        p.setSlug("test");
        p.setTitle("Two Sum");
        p.setDescription("desc");
        p.setDifficulty(difficulty);
        p.setTags(tags);
        p.setTimeLimitMs(2000);
        return p;
    }

    @Test
    void level1_restatesProblemAndMentionsTitle() {
        String h = engine.hint(problem(Difficulty.EASY, List.of("arrays")), 1);
        assertTrue(h.contains("Two Sum"), "should mention the title");
        assertTrue(h.toLowerCase().contains("brute-force"), "should nudge toward brute force");
        assertFalse(h.contains("```"), "must never contain a code block");
    }

    @Test
    void level2_pointsToATechnique() {
        String h = engine.hint(problem(Difficulty.MEDIUM, List.of("hashing")), 2);
        assertTrue(h.contains("hash map/set gives O(1)"));
        assertFalse(h.contains("```"));
    }

    @Test
    void level3_hasNumberedPlanWithoutCode() {
        String h = engine.hint(problem(Difficulty.HARD, List.of("dp")), 3);
        assertTrue(h.contains("1."));
        assertTrue(h.contains("2."));
        assertTrue(h.contains("5."));
        assertFalse(h.contains("```"), "level 3 outlines steps but never code");
    }

    @Test
    void hint_clampsLevelBelowOneToLevelOne() {
        Problem p = problem(Difficulty.EASY, List.of("arrays"));
        assertEquals(engine.hint(p, 1), engine.hint(p, 0));
        assertEquals(engine.hint(p, 1), engine.hint(p, -5));
    }

    @Test
    void hint_clampsLevelAboveThreeToLevelThree() {
        Problem p = problem(Difficulty.EASY, List.of("arrays"));
        assertEquals(engine.hint(p, 3), engine.hint(p, 4));
        assertEquals(engine.hint(p, 3), engine.hint(p, 99));
    }

    @Test
    void nullTags_areHandledGracefully() {
        String h = engine.hint(problem(Difficulty.EASY, null), 2);
        assertTrue(h.contains("Think about the core pattern"));
    }

    @Test
    void emptyTags_fallBackToBruteForceNudge() {
        String h = engine.hint(problem(Difficulty.EASY, List.of()), 2);
        assertTrue(h.contains("Start from the brute force"));
    }

    @ParameterizedTest
    @CsvSource({
            "hashing,        hash map/set gives O(1)",
            "two-pointer,    Two indices moving toward each other",
            "sliding-window, sliding window",
            "sorting,        Sorting first often exposes",
            "dp,             subproblem and a recurrence",
            "greedy,         local choice that provably",
            "graph,          Model it as nodes and edges",
            "binary-search,  binary-search the answer itself",
            "stack,          A stack captures",
            "queue,          A queue processes",
            "heap,           A heap keeps",
            "math,           closed-form formula",
            "string,         Scan the string once",
            "prefix-sum,     prefix sums",
            "quantumfoo,     Start from the brute force",
    })
    void techniqueMapping_perTag(String tag, String expectedFragment) {
        String h = engine.hint(problem(Difficulty.MEDIUM, List.of(tag)), 2);
        assertTrue(h.contains(expectedFragment.trim()),
                () -> "tag '" + tag + "' should map to a hint containing: " + expectedFragment);
    }
}
