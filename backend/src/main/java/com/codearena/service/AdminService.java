package com.codearena.service;

import com.codearena.dto.AdminContestRequest;
import com.codearena.dto.AdminProblemRequest;
import com.codearena.judge.Starters;
import com.codearena.model.Contest;
import com.codearena.model.Problem;
import com.codearena.model.TestCase;
import com.codearena.repository.ContestEntryRepository;
import com.codearena.repository.ContestRepository;
import com.codearena.repository.ProblemRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final ProblemRepository problemRepository;
    private final ContestRepository contestRepository;
    private final ContestEntryRepository contestEntryRepository;

    public AdminService(ProblemRepository problemRepository,
                        ContestRepository contestRepository,
                        ContestEntryRepository contestEntryRepository) {
        this.problemRepository = problemRepository;
        this.contestRepository = contestRepository;
        this.contestEntryRepository = contestEntryRepository;
    }

    @Transactional
    public Long createProblem(AdminProblemRequest req) {
        if (problemRepository.existsBySlug(req.slug())) {
            throw new IllegalStateException("A problem with slug '" + req.slug() + "' already exists");
        }
        Problem p = new Problem();
        applyProblem(p, req);
        return problemRepository.save(p).getId();
    }

    @Transactional
    public void updateProblem(String slug, AdminProblemRequest req) {
        Problem p = problemRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found: " + slug));
        p.getTestCases().clear();
        applyProblem(p, req);
        problemRepository.save(p);
    }

    @Transactional
    public void deleteProblem(String slug) {
        Problem p = problemRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found: " + slug));
        // Detach from any contests first to avoid FK violations.
        contestRepository.findAll().forEach(c -> {
            if (c.getProblems().removeIf(pr -> pr.getId().equals(p.getId()))) {
                contestRepository.save(c);
            }
        });
        problemRepository.delete(p);
    }

    @Transactional
    public Long createContest(AdminContestRequest req) {
        Contest c = new Contest();
        c.setName(req.name());
        c.setDescription(req.description());
        c.setStartTime(req.startTime());
        c.setEndTime(req.endTime());
        List<Problem> problems = new ArrayList<>();
        if (req.problemSlugs() != null) {
            for (String slug : req.problemSlugs()) {
                problems.add(problemRepository.findBySlug(slug)
                        .orElseThrow(() -> new ResourceNotFoundException("Problem not found: " + slug)));
            }
        }
        c.setProblems(problems);
        return contestRepository.save(c).getId();
    }

    @Transactional
    public void deleteContest(Long id) {
        Contest c = contestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contest not found: " + id));
        // Remove participant entries first to satisfy foreign keys.
        contestEntryRepository.findByContestId(id).forEach(contestEntryRepository::delete);
        contestRepository.delete(c);
    }

    private void applyProblem(Problem p, AdminProblemRequest req) {
        p.setSlug(req.slug());
        p.setTitle(req.title());
        p.setDifficulty(req.difficulty());
        p.setDescription(req.description());
        p.setTags(req.tags() != null ? req.tags() : List.of());
        p.setTimeLimitMs(req.timeLimitMs() != null && req.timeLimitMs() > 0 ? req.timeLimitMs() : 2000);
        p.setMemoryLimitMb(req.memoryLimitMb() != null && req.memoryLimitMb() > 0 ? req.memoryLimitMb() : 256);
        p.setStarterCode(Starters.defaults());
        if (req.testcases() != null) {
            for (AdminProblemRequest.TestCaseInput tc : req.testcases()) {
                p.addTestCase(new TestCase(
                        tc.input() == null ? "" : tc.input(),
                        tc.expectedOutput() == null ? "" : tc.expectedOutput(),
                        tc.sample()));
            }
        }
    }
}
