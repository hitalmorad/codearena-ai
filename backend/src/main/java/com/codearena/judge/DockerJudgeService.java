package com.codearena.judge;

import com.codearena.model.Language;
import com.codearena.model.Problem;
import com.codearena.model.TestCase;
import com.codearena.model.Verdict;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Runs untrusted submissions inside short-lived, locked-down Docker containers.
 *
 * <p>Hardening per run: no network, capped memory/swap, limited CPU and PIDs, a
 * read-only code mount for execution, and a host-enforced wall-clock timeout.
 * Supports interpreted (Python, JS) and compiled (Java, C++, C, Go) languages.
 */
@Service
public class DockerJudgeService implements JudgeEngine {

    private static final Logger log = LoggerFactory.getLogger(DockerJudgeService.class);
    private static final int MAX_OUTPUT_BYTES = 1_000_000;
    private static final long STARTUP_BUFFER_MS = 5_000;
    private static final long AVAILABILITY_TTL_MS = 5_000;

    private final String dockerCommand;
    private final Path workdir;
    private volatile long lastAvailabilityCheck = 0;
    private volatile boolean dockerAvailable = false;

    public DockerJudgeService(
            @Value("${codearena.judge.docker-command:docker}") String dockerCommand,
            @Value("${codearena.judge.workdir:${java.io.tmpdir}/codearena-judge}") String workdir) {
        this.dockerCommand = dockerCommand;
        this.workdir = Path.of(workdir);
    }

    @Override
    public String name() {
        return "docker";
    }

    @Override
    public boolean canRun(Language language) {
        // Docker can run any supported language (images are pulled on demand).
        return isAvailable();
    }

    @Override
    public boolean isAvailable() {
        long now = System.currentTimeMillis();
        if (now - lastAvailabilityCheck < AVAILABILITY_TTL_MS) {
            return dockerAvailable;
        }
        boolean ok = false;
        try {
            Process p = new ProcessBuilder(dockerCommand, "version", "--format", "{{.Server.Version}}")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = p.waitFor(4, TimeUnit.SECONDS);
            ok = finished && p.exitValue() == 0;
            if (!finished) {
                p.destroyForcibly();
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            ok = false;
        }
        dockerAvailable = ok;
        lastAvailabilityCheck = now;
        return ok;
    }

    @Override
    public JudgeResult judge(Language language, String sourceCode, Problem problem,
                             boolean sampleOnly, boolean stopOnFirstFailure) {
        Path runDir = null;
        try {
            Files.createDirectories(workdir);
            runDir = Files.createTempDirectory(workdir, "sub-");
            Files.writeString(runDir.resolve(language.sourceFile()), sourceCode, StandardCharsets.UTF_8);

            if (language.isCompiled()) {
                ExecResult compile = compile(language, runDir, problem);
                if (compile.timedOut() || compile.exitCode() != 0) {
                    String msg = compile.timedOut() ? "Compilation timed out." : trim(compile.stderr(), 4000);
                    return new JudgeResult(Verdict.COMPILATION_ERROR, 0, 0, null, msg, List.of());
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
            Verdict overall = Verdict.ACCEPTED;
            int failingIndex = -1;
            List<CaseResult> cases = new ArrayList<>();

            for (int i = 0; i < testCases.size(); i++) {
                TestCase tc = testCases.get(i);
                ExecResult run = run(language, runDir, problem, tc.getInput());
                int rt = (int) run.durationMs();
                maxRuntime = Math.max(maxRuntime, rt);

                Verdict caseVerdict;
                String actual;
                boolean ok = false;
                if (run.timedOut()) {
                    caseVerdict = Verdict.TIME_LIMIT_EXCEEDED;
                    actual = "";
                } else if (run.exitCode() == 137) {
                    caseVerdict = Verdict.MEMORY_LIMIT_EXCEEDED;
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
            log.error("Judge failed", e);
            return JudgeResult.internalError("Judge error: " + e.getMessage());
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

    private ExecResult compile(Language lang, Path runDir, Problem problem)
            throws IOException, InterruptedException {
        // Compilation gets generous memory (compilers need more than the run
        // limit) and a longer timeout to tolerate a cold Go build cache.
        List<String> cmd = new ArrayList<>(baseDockerArgs(runDir, 1024, false));
        cmd.add(lang.dockerImage());
        cmd.addAll(containerCompileArgs(lang));
        return execProcess(cmd, null, 60_000);
    }

    private ExecResult run(Language lang, Path runDir, Problem problem, String stdin)
            throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(baseDockerArgs(runDir, problem.getMemoryLimitMb(), true));
        cmd.add(lang.dockerImage());
        cmd.addAll(containerRunArgs(lang));
        long timeout = problem.getTimeLimitMs() + STARTUP_BUFFER_MS;
        return execProcess(cmd, stdin == null ? "" : stdin, timeout);
    }

    private List<String> containerCompileArgs(Language lang) {
        return switch (lang) {
            case JAVA -> List.of("javac", "/code/Main.java");
            case CPP -> List.of("g++", "-O2", "-std=c++17", "-o", "/code/prog", "/code/main.cpp");
            case C -> List.of("gcc", "-O2", "-o", "/code/prog", "/code/main.c");
            case GO -> List.of("go", "build", "-o", "/code/prog", "/code/main.go");
            default -> List.of();
        };
    }

    private List<String> containerRunArgs(Language lang) {
        return switch (lang) {
            case PYTHON -> List.of("python", "/code/main.py");
            case JAVASCRIPT -> List.of("node", "/code/main.js");
            case JAVA -> List.of("java", "-cp", "/code", "Main");
            case CPP, C, GO -> List.of("/code/prog");
        };
    }

    private List<String> baseDockerArgs(Path runDir, int memoryMb, boolean readOnly) {
        String mount = runDir.toAbsolutePath() + ":/code" + (readOnly ? ":ro" : "");
        return new ArrayList<>(List.of(
                dockerCommand, "run", "--rm", "-i",
                "--name", "codearena-" + UUID.randomUUID(),
                "--network", "none",
                "--memory", memoryMb + "m",
                "--memory-swap", memoryMb + "m",
                "--cpus", "2",
                "--pids-limit", "256",
                // Persist the Go build cache across runs so builds stay fast.
                "-v", "codearena-gocache:/root/.cache/go-build",
                "-v", mount,
                "-w", "/code"
        ));
    }

    private ExecResult execProcess(List<String> command, String stdin, long timeoutMs)
            throws IOException, InterruptedException {
        String containerName = extractContainerName(command);
        long start = System.nanoTime();
        Process process = new ProcessBuilder(command).start();

        Thread stdinThread = new Thread(() -> {
            try (OutputStream os = process.getOutputStream()) {
                if (stdin != null) {
                    os.write(stdin.getBytes(StandardCharsets.UTF_8));
                }
            } catch (IOException ignored) {
                // process may have exited before consuming input
            }
        });
        StreamCollector outCollector = new StreamCollector(process.getInputStream());
        StreamCollector errCollector = new StreamCollector(process.getErrorStream());
        Thread outThread = new Thread(outCollector);
        Thread errThread = new Thread(errCollector);
        stdinThread.start();
        outThread.start();
        errThread.start();

        boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        boolean timedOut = !finished;
        if (timedOut) {
            process.destroyForcibly();
            forceRemoveContainer(containerName);
        }
        stdinThread.join(1000);
        outThread.join(1000);
        errThread.join(1000);

        long durationMs = (System.nanoTime() - start) / 1_000_000;
        int exitCode = finished ? process.exitValue() : -1;
        return new ExecResult(exitCode, outCollector.text(), errCollector.text(), durationMs, timedOut);
    }

    private String extractContainerName(List<String> command) {
        int idx = command.indexOf("--name");
        return idx >= 0 && idx + 1 < command.size() ? command.get(idx + 1) : null;
    }

    private void forceRemoveContainer(String name) {
        if (name == null) {
            return;
        }
        try {
            new ProcessBuilder(dockerCommand, "rm", "-f", name)
                    .redirectErrorStream(true)
                    .start()
                    .waitFor(5, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Failed to force-remove container {}", name);
        }
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
