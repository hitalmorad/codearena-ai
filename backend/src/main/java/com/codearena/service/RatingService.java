package com.codearena.service;

import com.codearena.model.Difficulty;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Rating math for CodeArena.
 *
 * <ul>
 *   <li>Solving a problem for the first time grants a difficulty-weighted bonus.</li>
 *   <li>Finishing a contest applies a multiplayer Elo adjustment based on rank.</li>
 * </ul>
 */
@Service
public class RatingService {

    public static final int MIN_RATING = 800;
    private static final int ELO_K = 40;

    /** One-time rating bonus for first solving a problem of the given difficulty. */
    public int solveReward(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 15;
            case MEDIUM -> 30;
            case HARD -> 50;
        };
    }

    /** A contestant's rating, solved count and penalty used for Elo. */
    public record Contestant(Long entryId, int rating, int solved, long penalty) {
    }

    /** Rating delta for one contestant. */
    public record Delta(Long entryId, int delta) {
    }

    /**
     * Computes Elo deltas for all contestants. Each contestant is compared
     * pairwise against every other; a better final rank counts as a win.
     */
    public List<Delta> computeContestDeltas(List<Contestant> contestants) {
        int n = contestants.size();
        if (n < 2) {
            return contestants.stream().map(c -> new Delta(c.entryId(), 0)).toList();
        }
        return contestants.stream().map(me -> {
            double expected = 0;
            double actual = 0;
            for (Contestant other : contestants) {
                if (other.entryId().equals(me.entryId())) {
                    continue;
                }
                expected += 1.0 / (1.0 + Math.pow(10.0, (other.rating() - me.rating()) / 400.0));
                int cmp = compareRank(me, other);
                actual += cmp > 0 ? 1.0 : (cmp == 0 ? 0.5 : 0.0);
            }
            int delta = (int) Math.round(ELO_K * (actual - expected) / (n - 1));
            return new Delta(me.entryId(), delta);
        }).toList();
    }

    /** Positive if {@code a} ranks strictly better than {@code b}. */
    private int compareRank(Contestant a, Contestant b) {
        if (a.solved() != b.solved()) {
            return Integer.compare(a.solved(), b.solved()); // more solved = better
        }
        return Long.compare(b.penalty(), a.penalty()); // less penalty = better
    }
}
