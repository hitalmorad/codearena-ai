package com.codearena.service;

import com.codearena.dto.JudgeReportDto;
import com.codearena.dto.SubmissionRequest;
import com.codearena.dto.SubmissionResponse;
import com.codearena.judge.CaseResult;
import com.codearena.judge.JudgeResult;
import com.codearena.judge.JudgeService;
import com.codearena.model.Problem;
import com.codearena.model.Submission;
import com.codearena.model.User;
import com.codearena.model.Verdict;
import com.codearena.repository.SubmissionRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemService problemService;
    private final JudgeService judgeService;
    private final UserService userService;
    private final ContestService contestService;
    private final AuthService authService;

    public SubmissionService(SubmissionRepository submissionRepository,
                             ProblemService problemService,
                             JudgeService judgeService,
                             UserService userService,
                             ContestService contestService,
                             AuthService authService) {
        this.submissionRepository = submissionRepository;
        this.problemService = problemService;
        this.judgeService = judgeService;
        this.userService = userService;
        this.contestService = contestService;
        this.authService = authService;
    }

    /**
     * "Run": judges only the sample cases and reveals their input / expected /
     * actual output (LeetCode-style). Not persisted, no rating impact.
     */
    @Transactional
    public JudgeReportDto run(String slug, SubmissionRequest request) {
        Problem problem = problemService.requireBySlug(slug);
        problem.getTestCases().size();
        JudgeResult result = judgeService.judge(request.language(), request.sourceCode(), problem, true, false);
        return report(result, false, true);
    }

    /**
     * "Submit": judges all cases (stopping at the first failure), persists the
     * submission, and on an accepted first solve updates rating, leaderboard and
     * live contest standings. Case details are hidden in contest mode and, in
     * practice mode, only sample cases and the failing case are revealed.
     */
    @Transactional
    public JudgeReportDto submit(String slug, SubmissionRequest request, String authToken) {
        Problem problem = problemService.requireBySlug(slug);
        // Touch lazy collections so they are available to the judge.
        problem.getTestCases().size();

        // The acting user is resolved from the auth token (authoritative) or,
        // as a fallback, an existing account named in the request.
        User actingUser = authService.resolveToken(authToken)
                .or(() -> userService.find(hasText(request.username()) ? request.username().trim() : null))
                .orElse(null);

        boolean contestMode = actingUser != null
                && contestService.inActiveContest(actingUser.getUsername(), problem.getId());

        JudgeResult result = judgeService.judge(request.language(), request.sourceCode(), problem, false, true);

        Submission submission = new Submission();
        submission.setProblem(problem);
        submission.setUser(actingUser);
        submission.setLanguage(request.language());
        submission.setSourceCode(request.sourceCode());
        submission.setVerdict(result.verdict());
        submission.setPassedTests(result.passedTests());
        submission.setTotalTests(result.totalTests());
        submission.setRuntimeMs(result.runtimeMs());
        submission.setMessage(result.message());
        submissionRepository.save(submission);

        if (result.verdict() == Verdict.ACCEPTED && actingUser != null) {
            boolean newlySolved = userService.applyGlobalSolve(actingUser, problem);
            contestService.recordContestSolve(actingUser, problem);
            if (newlySolved) {
                userService.broadcastLeaderboard();
            }
        }

        return report(result, contestMode, false);
    }

    /**
     * Builds the client report, deciding which case details to reveal.
     *
     * <ul>
     *   <li><b>Run</b> ({@code revealAll}): every sample case is shown.</li>
     *   <li><b>Practice submit</b>: sample cases and the single failing case are
     *       revealed (LeetCode-style), with the full passed/total count.</li>
     *   <li><b>Contest submit</b>: no per-case data at all — only the overall
     *       verdict as a plain message (e.g. "Wrong Answer"), like Codeforces.</li>
     * </ul>
     *
     * @param revealAll reveal every case (used for "Run" on sample cases)
     */
    private JudgeReportDto report(JudgeResult r, boolean contestMode, boolean revealAll) {
        List<JudgeReportDto.CaseView> views = new ArrayList<>();
        if (!contestMode) {
            for (CaseResult c : r.cases()) {
                // Run: show every sample case. Practice submit: show ONLY the
                // failing case (the passed/total count conveys the rest).
                boolean include = revealAll || !c.passed();
                if (!include) {
                    continue;
                }
                views.add(new JudgeReportDto.CaseView(
                        c.index(), c.sample(), c.passed(), c.verdict(),
                        c.input(), c.expectedOutput(), c.actualOutput(), c.runtimeMs()));
            }
        }
        // In a live contest we expose nothing about the tests — just the verdict.
        String message = contestMode ? contestMessage(r.verdict()) : r.message();
        return new JudgeReportDto(r.verdict().name(), r.passedTests(), r.totalTests(),
                r.runtimeMs(), message, contestMode, views);
    }

    /** A test-data-free verdict message shown during contests. */
    private String contestMessage(Verdict v) {
        return switch (v) {
            case ACCEPTED -> "Accepted";
            case WRONG_ANSWER -> "Wrong Answer";
            case TIME_LIMIT_EXCEEDED -> "Time Limit Exceeded";
            case MEMORY_LIMIT_EXCEEDED -> "Memory Limit Exceeded";
            case RUNTIME_ERROR -> "Runtime Error";
            case COMPILATION_ERROR -> "Compilation Error";
            default -> "Rejected";
        };
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> recentForProblem(String slug) {
        Problem problem = problemService.requireBySlug(slug);
        return submissionRepository.findTop20ByProblemIdOrderByCreatedAtDesc(problem.getId()).stream()
                .map(SubmissionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> historyFor(String username) {
        User user = userService.requireByUsername(username);
        return submissionRepository.findTop50ByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(SubmissionResponse::from)
                .toList();
    }
}
