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
     * @param revealAll  reveal every case (used for "Run" on sample cases)
     */
    private JudgeReportDto report(JudgeResult r, boolean contestMode, boolean revealAll) {
        List<JudgeReportDto.CaseView> views = new ArrayList<>();
        for (CaseResult c : r.cases()) {
            // Practice: show sample cases and the failing case. Contest: hide all I/O.
            boolean reveal = revealAll || (!contestMode && (c.sample() || !c.passed()));
            views.add(new JudgeReportDto.CaseView(
                    c.index(), c.sample(), c.passed(), c.verdict(),
                    reveal ? c.input() : null,
                    reveal ? c.expectedOutput() : null,
                    reveal ? c.actualOutput() : null,
                    c.runtimeMs()));
        }
        return new JudgeReportDto(r.verdict().name(), r.passedTests(), r.totalTests(),
                r.runtimeMs(), r.message(), contestMode, views);
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
