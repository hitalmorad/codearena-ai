package com.codearena.controller;

import com.codearena.dto.JudgeReportDto;
import com.codearena.dto.ProblemDetailDto;
import com.codearena.dto.ProblemSummaryDto;
import com.codearena.dto.SubmissionRequest;
import com.codearena.dto.SubmissionResponse;
import com.codearena.service.ProblemService;
import com.codearena.service.SubmissionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemService problemService;
    private final SubmissionService submissionService;

    public ProblemController(ProblemService problemService, SubmissionService submissionService) {
        this.problemService = problemService;
        this.submissionService = submissionService;
    }

    @GetMapping
    public List<ProblemSummaryDto> list() {
        return problemService.listProblems();
    }

    @GetMapping("/{slug}")
    public ProblemDetailDto detail(@PathVariable String slug) {
        return problemService.getProblemDetail(slug);
    }

    @PostMapping("/{slug}/run")
    public JudgeReportDto run(@PathVariable String slug, @Valid @RequestBody SubmissionRequest request) {
        return submissionService.run(slug, request);
    }

    @PostMapping("/{slug}/submit")
    public JudgeReportDto submit(@PathVariable String slug,
                                 @RequestHeader(value = "X-Auth-Token", required = false) String token,
                                 @Valid @RequestBody SubmissionRequest request) {
        return submissionService.submit(slug, request, token);
    }

    @GetMapping("/{slug}/submissions")
    public List<SubmissionResponse> submissions(@PathVariable String slug) {
        return submissionService.recentForProblem(slug);
    }
}
