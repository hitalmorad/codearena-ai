package com.codearena.ai;

import com.codearena.model.Problem;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Offline, model-free hint generator. Produces tiered Socratic-style nudges
 * from a problem's difficulty and topic tags. Used whenever the Groq provider
 * is disabled or unreachable, so the app always offers useful guidance.
 */
@Component
public class HeuristicHintEngine {

    public String hint(Problem p, int level) {
        int lvl = Math.max(1, Math.min(3, level));
        return switch (lvl) {
            case 1 -> level1(p);
            case 2 -> level2(p);
            default -> level3(p);
        };
    }

    private String level1(Problem p) {
        return "Re-read **" + p.getTitle() + "** (" + p.getDifficulty().name().toLowerCase(Locale.ROOT) + "). "
                + "Restate the input and the exact output format in your own words. "
                + "List the edge cases before coding: empty input, a single element, duplicates, "
                + "negative numbers, and the largest allowed size (time limit " + p.getTimeLimitMs() + " ms). "
                + "Question: what is the simplest brute-force that would definitely be correct?";
    }

    private String level2(Problem p) {
        String technique = techniqueFor(p.getTags());
        return "Now think about efficiency. " + technique + " "
                + "Ask yourself which part of your brute-force repeats work, and whether the right "
                + "data structure or ordering removes that repetition.";
    }

    private String level3(Problem p) {
        String technique = techniqueFor(p.getTags());
        return "Plan (in words, no code):\n"
                + "1. Parse the input exactly as specified.\n"
                + "2. " + technique + "\n"
                + "3. Apply it in a single pass or with the chosen structure to build the answer.\n"
                + "4. Handle the edge cases you listed earlier.\n"
                + "5. Print the result in the exact required format.\n"
                + "Then translate each step into your language and test on the sample cases.";
    }

    private String techniqueFor(List<String> tags) {
        if (tags == null) {
            return "Think about the core pattern before optimizing.";
        }
        for (String raw : tags) {
            String t = raw.toLowerCase(Locale.ROOT);
            if (t.contains("hash")) return "A hash map/set gives O(1) lookups — can you trade memory for speed?";
            if (t.contains("two-pointer")) return "Two indices moving toward each other can replace a nested loop.";
            if (t.contains("sliding")) return "A sliding window maintains a running range without recomputing from scratch.";
            if (t.contains("sort")) return "Sorting first often exposes structure that makes the rest linear.";
            if (t.contains("binary-search") || t.equals("search")) return "If the answer is monotonic, binary-search the answer itself.";
            if (t.contains("dp") || t.contains("dynamic")) return "Define a subproblem and a recurrence; memoize overlapping states.";
            if (t.contains("greedy")) return "Look for a local choice that provably stays globally optimal.";
            if (t.contains("graph") || t.contains("bfs") || t.contains("dfs")) return "Model it as nodes and edges; BFS finds shortest unweighted paths, DFS explores fully.";
            if (t.contains("stack")) return "A stack captures most-recent / matching semantics (e.g. brackets).";
            if (t.contains("queue")) return "A queue processes items in arrival order — useful for BFS or streaming.";
            if (t.contains("heap") || t.contains("priority")) return "A heap keeps the min/max reachable in O(log n).";
            if (t.contains("math") || t.contains("number")) return "Look for a closed-form formula or a number-theory property.";
            if (t.contains("string")) return "Scan the string once; prefix counts or a frequency table often suffice.";
            if (t.contains("prefix")) return "Precompute prefix sums so any range query is O(1).";
        }
        return "Start from the brute force, then remove the most repeated work.";
    }
}
