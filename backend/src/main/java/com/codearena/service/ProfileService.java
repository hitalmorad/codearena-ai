package com.codearena.service;

import com.codearena.dto.ActivityDto;
import com.codearena.dto.ProfileDto;
import com.codearena.model.Difficulty;
import com.codearena.model.Problem;
import com.codearena.model.Submission;
import com.codearena.model.User;
import com.codearena.model.Verdict;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.SubmissionRepository;
import com.codearena.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the aggregate stats shown on a user's profile: solved-by-difficulty,
 * acceptance rate and global rank.
 */
@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;

    public ProfileService(UserRepository userRepository,
                          ProblemRepository problemRepository,
                          SubmissionRepository submissionRepository) {
        this.userRepository = userRepository;
        this.problemRepository = problemRepository;
        this.submissionRepository = submissionRepository;
    }

    @Transactional(readOnly = true)
    public ProfileDto build(String username) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        List<Problem> solved = problemRepository.findAllById(u.getSolvedProblemIds());
        long solvedEasy = solved.stream().filter(p -> p.getDifficulty() == Difficulty.EASY).count();
        long solvedMedium = solved.stream().filter(p -> p.getDifficulty() == Difficulty.MEDIUM).count();
        long solvedHard = solved.stream().filter(p -> p.getDifficulty() == Difficulty.HARD).count();

        long totalEasy = problemRepository.countByDifficulty(Difficulty.EASY);
        long totalMedium = problemRepository.countByDifficulty(Difficulty.MEDIUM);
        long totalHard = problemRepository.countByDifficulty(Difficulty.HARD);

        long totalSubmissions = submissionRepository.countByUserId(u.getId());
        long acceptedSubmissions = submissionRepository.countByUserIdAndVerdict(u.getId(), Verdict.ACCEPTED);

        int rank = (int) userRepository.countByRatingGreaterThan(u.getRating()) + 1;
        long totalUsers = userRepository.count();

        return new ProfileDto(
                u.getUsername(), u.getBio(), u.getRating(), rank, totalUsers,
                solvedEasy, solvedMedium, solvedHard,
                totalEasy, totalMedium, totalHard,
                totalSubmissions, acceptedSubmissions);
    }

    /** Daily submission counts over the past year (only non-empty days). */
    @Transactional(readOnly = true)
    public List<ActivityDto> activity(String username) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        Instant since = Instant.now().minus(365, ChronoUnit.DAYS);
        ZoneId zone = ZoneId.systemDefault();
        Map<LocalDate, Long> byDay = submissionRepository
                .findByUserIdAndCreatedAtAfter(u.getId(), since).stream()
                .collect(Collectors.groupingBy(
                        (Submission s) -> s.getCreatedAt().atZone(zone).toLocalDate(),
                        TreeMap::new,
                        Collectors.counting()));
        return byDay.entrySet().stream()
                .map(e -> new ActivityDto(e.getKey().toString(), e.getValue()))
                .collect(Collectors.toList());
    }
}
