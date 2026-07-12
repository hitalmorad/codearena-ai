package com.codearena.judge;

import com.codearena.model.Language;
import com.codearena.model.Problem;

/**
 * A pluggable execution backend for the judge. Implementations run a
 * submission's source against a problem's test cases and return a verdict.
 *
 * <p>{@link DockerJudgeService} is the secure, production-grade engine.
 * {@link LocalProcessJudgeEngine} is an insecure host-execution fallback used
 * only when Docker is unavailable on a developer machine.
 */
public interface JudgeEngine {

    /**
     * Runs the submission.
     *
     * @param sampleOnly          run only the problem's sample cases (for "Run")
     * @param stopOnFirstFailure  stop at the first failing case (for "Submit")
     */
    JudgeResult judge(Language language, String sourceCode, Problem problem,
                      boolean sampleOnly, boolean stopOnFirstFailure);

    /** Whether this engine can run on the current host right now. */
    boolean isAvailable();

    /** Whether this engine can execute the given language right now. */
    boolean canRun(Language language);

    /** Short human-readable name for logging. */
    String name();
}
