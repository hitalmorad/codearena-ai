package com.codearena.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codearena.model.Difficulty;
import com.codearena.service.RatingService.Contestant;
import com.codearena.service.RatingService.Delta;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class RatingServiceTest {

    private final RatingService rating = new RatingService();

    @Test
    void solveReward_isDifficultyWeighted() {
        assertEquals(15, rating.solveReward(Difficulty.EASY));
        assertEquals(30, rating.solveReward(Difficulty.MEDIUM));
        assertEquals(50, rating.solveReward(Difficulty.HARD));
    }

    @Test
    void singleContestant_getsNoDelta() {
        List<Delta> deltas = rating.computeContestDeltas(
                List.of(new Contestant(1L, 1500, 3, 20)));
        assertEquals(1, deltas.size());
        assertEquals(0, deltas.get(0).delta());
    }

    @Test
    void winnerGainsAndLoserLoses_symmetricallyForEqualRatings() {
        // Same rating; player 1 solved more -> ranks better.
        List<Delta> deltas = rating.computeContestDeltas(List.of(
                new Contestant(1L, 1500, 2, 30),
                new Contestant(2L, 1500, 1, 10)));

        Map<Long, Integer> byId = deltas.stream()
                .collect(Collectors.toMap(Delta::entryId, Delta::delta));

        assertEquals(20, byId.get(1L), "winner gains K*(1-0.5) = 20");
        assertEquals(-20, byId.get(2L), "loser drops by the same amount");
    }

    @Test
    void tieOnSolved_isBrokenByLowerPenalty() {
        List<Delta> deltas = rating.computeContestDeltas(List.of(
                new Contestant(1L, 1500, 2, 5),   // fewer penalty minutes -> better
                new Contestant(2L, 1500, 2, 50)));

        Map<Long, Integer> byId = deltas.stream()
                .collect(Collectors.toMap(Delta::entryId, Delta::delta));

        assertTrue(byId.get(1L) > 0, "lower penalty should gain rating");
        assertTrue(byId.get(2L) < 0, "higher penalty should lose rating");
    }

    @Test
    void deltasAcrossAllContestants_sumToApproximatelyZero() {
        List<Delta> deltas = rating.computeContestDeltas(List.of(
                new Contestant(1L, 1600, 3, 40),
                new Contestant(2L, 1500, 2, 30),
                new Contestant(3L, 1400, 1, 20),
                new Contestant(4L, 1450, 0, 0)));

        int sum = deltas.stream().mapToInt(Delta::delta).sum();
        // rounding can leave a tiny residue, but it should be near zero
        assertTrue(Math.abs(sum) <= 2, "zero-sum property (allowing rounding): " + sum);
    }

    @Test
    void higherRatedFavourite_gainsLessForWinningThanAnUnderdog() {
        Function<List<Contestant>, Map<Long, Integer>> run = list ->
                rating.computeContestDeltas(list).stream()
                        .collect(Collectors.toMap(Delta::entryId, Delta::delta));

        // Favourite (1800) beats underdog (1200)
        Map<Long, Integer> favWins = run.apply(List.of(
                new Contestant(1L, 1800, 1, 10),
                new Contestant(2L, 1200, 0, 0)));
        // Underdog (1200) beats favourite (1800)
        Map<Long, Integer> underdogWins = run.apply(List.of(
                new Contestant(1L, 1200, 1, 10),
                new Contestant(2L, 1800, 0, 0)));

        assertTrue(underdogWins.get(1L) > favWins.get(1L),
                "an underdog win should be worth more than a favourite win");
    }
}
