package com.codearena.service;

import com.codearena.dto.ProblemDetailDto;
import com.codearena.dto.ProblemSummaryDto;
import com.codearena.model.Problem;
import com.codearena.repository.ProblemRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProblemService {

    private final ProblemRepository problemRepository;

    public ProblemService(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    @Transactional(readOnly = true)
    public List<ProblemSummaryDto> listProblems() {
        return problemRepository.findAll().stream()
                .map(p -> new ProblemSummaryDto(p.getId(), p.getSlug(), p.getTitle(), p.getDifficulty(), p.getTags()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProblemDetailDto getProblemDetail(String slug) {
        Problem p = requireBySlug(slug);
        List<ProblemDetailDto.SampleTestCaseDto> samples = p.getTestCases().stream()
                .filter(tc -> tc.isSample())
                .map(tc -> new ProblemDetailDto.SampleTestCaseDto(tc.getInput(), tc.getExpectedOutput()))
                .toList();
        return new ProblemDetailDto(
                p.getId(), p.getSlug(), p.getTitle(), p.getDescription(), p.getDifficulty(),
                p.getTags(), p.getStarterCode(), p.getTimeLimitMs(), p.getMemoryLimitMb(), samples);
    }

    @Transactional(readOnly = true)
    public Problem requireBySlug(String slug) {
        return problemRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found: " + slug));
    }
}
