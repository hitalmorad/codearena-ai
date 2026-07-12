package com.codearena.judge;

import com.codearena.model.Language;
import com.codearena.model.Problem;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Entry point used by the application to judge submissions. It selects a
 * concrete {@link JudgeEngine} per submission based on which engine can run the
 * requested language.
 *
 * <p>By default the fast local runtime is preferred when it can run the
 * language (e.g. Python, JavaScript, Java on this host), falling back to the
 * Docker sandbox for languages without a local toolchain (e.g. C/C++/Go). Set
 * {@code codearena.judge.prefer=docker} to always prefer the secure sandbox.
 */
@Service
public class JudgeService {

    private static final Logger log = LoggerFactory.getLogger(JudgeService.class);

    private final List<JudgeEngine> engines;

    public JudgeService(DockerJudgeService dockerEngine,
                        LocalProcessJudgeEngine localEngine,
                        @Value("${codearena.judge.prefer:local}") String prefer) {
        this.engines = "docker".equalsIgnoreCase(prefer)
                ? List.of(dockerEngine, localEngine)
                : List.of(localEngine, dockerEngine);
    }

    public JudgeResult judge(Language language, String sourceCode, Problem problem,
                             boolean sampleOnly, boolean stopOnFirstFailure) {
        for (JudgeEngine engine : engines) {
            if (engine.canRun(language)) {
                log.debug("Judging {} submission with {} engine", language, engine.name());
                return engine.judge(language, sourceCode, problem, sampleOnly, stopOnFirstFailure);
            }
        }
        return JudgeResult.internalError(
                language + " can't be executed here. Install its local runtime, or start Docker Desktop "
                + "and pull the language image.");
    }
}
