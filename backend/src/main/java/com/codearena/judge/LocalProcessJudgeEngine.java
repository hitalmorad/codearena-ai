package com.codearena.judge;

import com.codearena.model.Language;
import com.codearena.model.Problem;
import com.codearena.model.TestCase;
import com.codearena.model.Verdict;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Runs submissions directly on the host toolchain.
 *
 * <p><strong>No sandboxing.</strong> Developer-machine fallback only, selected
 * by {@link JudgeService} when Docker is unavailable. Each language needs its
 * runtime installed locally; if a runtime is missing the engine returns a clear
 * message telling the user to start Docker or install it.
 */
@Service
public class LocalProcessJudgeEngine implements JudgeEngine {

    private static final Logger log = LoggerFactory.getLogger(LocalProcessJudgeEngine.class);
    private static final int MAX_OUTPUT_BYTES = 1_000_000;
    private static final long STARTUP_BUFFER_MS = 3_000;

    private final String python;
    private final String node;
    private final String java;
    private final String javac;
    private final String gpp;
    private final String gcc;
    private final String go;
    private final Path workdir;

    public LocalProcessJudgeEngine(
            @Value("${codearena.judge.local.python:python}") String python,
            @Value("${codearena.judge.local.node:node}") String node,
            @Value("${codearena.judge.local.java:java}") String java,
            @Value("${codearena.judge.local.javac:javac}") String javac,
            @Value("${codearena.judge.local.gpp:g++}") String gpp,
            @Value("${codearena.judge.local.gcc:gcc}") String gcc,
            @Value("${codearena.judge.local.go:go}") String go,
            @Value("${codearena.judge.workdir:${java.io.tmpdir}/codearena-judge}") String workdir) {
        this.python = python;
        this.node = node;
        this.java = java;
        this.javac = javac;
        this.gpp = gpp;
        this.gcc = gcc;
        this.go = go;
        this.workdir = Path.of(workdir);
    }

    @Override
    public String name() {
        return "local-process (INSECURE dev fallback)";
    }

    @Override
    public boolean isAvailable() {
        // Available if at least one common runtime is present.
        return commandExists(python, "--version")
                || commandExists(node, "--version")
                || commandExists(javac, "-version");
    }

    @Override
    public boolean canRun(Language language) {
        String required = requiredCommand(language);
        String versionFlag = language == Language.JAVA ? "-version" : "--version";
        return commandExists(required, versionFlag);
    }

    @Override
    public JudgeResult judge(Language language, String sourceCode, Problem problem,
                             boolean sampleOnly, boolean stopOnFirstFailure) {
        String required = requiredCommand(language);
        String versionFlag = language == Language.JAVA ? "-version" : "--version";
        if (!commandExists(required, versionFlag)) {
            return JudgeResult.internalError(
                    displayName(language) + " isn't runnable here — '" + required
                    + "' is not installed. Start Docker Desktop, or install " + required + " locally.");
        }

        log.warn("Judging with the INSECURE local-process engine (Docker unavailable). Dev use only.");
        Path runDir = null;
        try {
            Files.createDirectories(workdir);
            runDir = Files.createTempDirectory(workdir, "local-");
            Files.writeString(runDir.resolve(language.sourceFile()), sourceCode, StandardCharsets.UTF_8);

            if (language.isCompiled()) {
                List<String> compileCmd = compileCommand(language, runDir);
                if (compileCmd != null) {
                    ExecResult compile = exec(compileCmd, runDir, null, 25_000);
                    if (compile.timedOut() || compile.exitCode() != 0) {
                        return new JudgeResult(Verdict.COMPILATION_ERROR, 0, 0, null,
                                compile.timedOut() ? "Compilation timed out." : trim(compile.stderr(), 4000),
                                List.of());
                    }
                }
            }

            List<TestCase> testCases = new ArrayList<>(problem.getTestCases());
            testCases.sort(Comparator.comparing(TestCase::getId));
            if (sampleOnly) {
                testCases.removeIf(tc -> !tc.isSample());
            }

            int total = testCases.size();
            int passed = 0;
            int maxRuntime = 0;
            int failingIndex = -1;
            Verdict overall = Verdict.ACCEPTED;
            List<CaseResult> cases = new ArrayList<>();
            long timeout = problem.getTimeLimitMs() + STARTUP_BUFFER_MS;

            for (int i = 0; i < testCases.size(); i++) {
                TestCase tc = testCases.get(i);
                ExecResult run = exec(runCommand(language, runDir), runDir, tc.getInput(), timeout);
                int rt = (int) run.durationMs();
                maxRuntime = Math.max(maxRuntime, rt);

                Verdict caseVerdict;
                String actual;
                boolean ok = false;
                if (run.timedOut()) {
                    caseVerdict = Verdict.TIME_LIMIT_EXCEEDED;
                    actual = "";
                } else if (run.exitCode() != 0) {
                    caseVerdict = Verdict.RUNTIME_ERROR;
                    actual = trim(run.stderr(), 2000);
                } else if (!OutputMatcher.matches(tc.getExpectedOutput(), run.stdout())) {
                    caseVerdict = Verdict.WRONG_ANSWER;
                    actual = trim(run.stdout(), 2000);
                } else {
                    caseVerdict = Verdict.ACCEPTED;
                    actual = trim(run.stdout(), 2000);
                    ok = true;
                }

                cases.add(new CaseResult(i + 1, tc.isSample(), ok, caseVerdict.name(),
                        tc.getInput(), tc.getExpectedOutput(), actual, rt));

                if (ok) {
                    passed++;
                } else if (overall == Verdict.ACCEPTED) {
                    overall = caseVerdict;
                    failingIndex = i + 1;
                    if (stopOnFirstFailure) {
                        break;
                    }
                }
            }

            String message = overall == Verdict.ACCEPTED
                    ? "All tests passed."
                    : humanVerdict(overall) + " on test " + failingIndex;
            return new JudgeResult(overall, passed, total, maxRuntime, message, cases);
        } catch (Exception e) {
            log.error("Local judge failed", e);
            return JudgeResult.internalError("Local judge error: " + e.getMessage());
        } finally {
            deleteQuietly(runDir);
        }
    }

    private String humanVerdict(Verdict v) {
        return switch (v) {
            case WRONG_ANSWER -> "Wrong answer";
            case TIME_LIMIT_EXCEEDED -> "Time limit exceeded";
            case MEMORY_LIMIT_EXCEEDED -> "Memory limit exceeded";
            case RUNTIME_ERROR -> "Runtime error";
            case COMPILATION_ERROR -> "Compilation error";
            default -> "Failed";
        };
    }

    private List<String> compileCommand(Language lang, Path dir) {
        return switch (lang) {
            case JAVA -> List.of(javac, "Main.java");
            case CPP -> List.of(gpp, "-O2", "-std=c++17", "-o", "prog.exe", "main.cpp");
            case C -> List.of(gcc, "-O2", "-o", "prog.exe", "main.c");
            case GO -> List.of(go, "build", "-o", "prog.exe", "main.go");
            default -> null;
        };
    }

    private List<String> runCommand(Language lang, Path dir) {
        return switch (lang) {
            case PYTHON -> List.of(python, "main.py");
            case JAVASCRIPT -> List.of(node, "main.js");
            case JAVA -> List.of(java, "-cp", ".", "Main");
            case CPP, C, GO -> List.of(dir.resolve("prog.exe").toString());
        };
    }

    private String requiredCommand(Language lang) {
        return switch (lang) {
            case PYTHON -> python;
            case JAVASCRIPT -> node;
            case JAVA -> javac;
            case CPP -> gpp;
            case C -> gcc;
            case GO -> go;
        };
    }

    private String displayName(Language lang) {
        return switch (lang) {
            case PYTHON -> "Python";
            case JAVASCRIPT -> "JavaScript";
            case JAVA -> "Java";
            case CPP -> "C++";
            case C -> "C";
            case GO -> "Go";
        };
    }

    private boolean commandExists(String command, String versionFlag) {
        try {
            Process p = new ProcessBuilder(command, versionFlag).redirectErrorStream(true).start();
            boolean finished = p.waitFor(4, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                return false;
            }
            return p.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private ExecResult exec(List<String> command, Path runDir, String stdin, long timeoutMs)
            throws IOException, InterruptedException {
        // Feed stdin from a real file rather than streaming through the process
        // pipe. This is robust across languages on Windows (e.g. Node's
        // readFileSync(0) can hang on a Java ProcessBuilder pipe with EAGAIN).
        Path stdinFile = runDir.resolve("__stdin.txt");
        Files.writeString(stdinFile, stdin == null ? "" : stdin, StandardCharsets.UTF_8);

        long start = System.nanoTime();
        ProcessBuilder pb = new ProcessBuilder(command).directory(runDir.toFile());
        pb.redirectInput(stdinFile.toFile());
        Process process = pb.start();

        StreamCollector out = new StreamCollector(process.getInputStream());
        StreamCollector err = new StreamCollector(process.getErrorStream());
        Thread outThread = new Thread(out);
        Thread errThread = new Thread(err);
        outThread.start();
        errThread.start();

        boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        boolean timedOut = !finished;
        if (timedOut) {
            process.destroyForcibly();
        }
        outThread.join(1000);
        errThread.join(1000);

        long durationMs = (System.nanoTime() - start) / 1_000_000;
        int exitCode = finished ? process.exitValue() : -1;
        log.debug("exec {} -> finished={} exit={} ms={}", command, finished, exitCode, durationMs);
        return new ExecResult(exitCode, out.text(), err.text(), durationMs, timedOut);
    }

    private String trim(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best-effort cleanup
                }
            });
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }

    private static final class StreamCollector implements Runnable {
        private final InputStream in;
        private final StringBuilder sb = new StringBuilder();

        StreamCollector(InputStream in) {
            this.in = in;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[8192];
            int total = 0;
            try {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    if (total < MAX_OUTPUT_BYTES) {
                        int allowed = Math.min(read, MAX_OUTPUT_BYTES - total);
                        sb.append(new String(buffer, 0, allowed, StandardCharsets.UTF_8));
                        total += allowed;
                    }
                }
            } catch (IOException ignored) {
                // stream closed when process ends
            }
        }

        String text() {
            return sb.toString();
        }
    }

    private record ExecResult(int exitCode, String stdout, String stderr, long durationMs, boolean timedOut) {
    }
}
