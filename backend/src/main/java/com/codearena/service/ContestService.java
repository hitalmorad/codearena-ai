package com.codearena.service;

import com.codearena.dto.ContestDetailDto;
import com.codearena.dto.ContestHistoryDto;
import com.codearena.dto.ContestSummaryDto;
import com.codearena.dto.StandingRowDto;
import com.codearena.model.Contest;
import com.codearena.model.ContestEntry;
import com.codearena.model.Problem;
import com.codearena.model.Role;
import com.codearena.model.User;
import com.codearena.realtime.SseHub;
import com.codearena.repository.ContestEntryRepository;
import com.codearena.repository.ContestRepository;
import com.codearena.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContestService {

    private final ContestRepository contestRepository;
    private final ContestEntryRepository entryRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final RatingService ratingService;
    private final SseHub sseHub;

    public ContestService(ContestRepository contestRepository,
                          ContestEntryRepository entryRepository,
                          UserRepository userRepository,
                          UserService userService,
                          RatingService ratingService,
                          SseHub sseHub) {
        this.contestRepository = contestRepository;
        this.entryRepository = entryRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.ratingService = ratingService;
        this.sseHub = sseHub;
    }

    @Transactional(readOnly = true)
    public List<ContestSummaryDto> listContests() {
        Instant now = Instant.now();
        return contestRepository.findAllByOrderByStartTimeDesc().stream()
                .map(c -> new ContestSummaryDto(
                        c.getId(), c.getName(), c.statusAt(now), c.getStartTime(), c.getEndTime(),
                        c.getProblems().size(), entryRepository.countByContestId(c.getId())))
                .toList();
    }

    @Transactional
    public ContestDetailDto getContest(Long id, String username) {
        Contest c = requireContest(id);
        finalizeIfEnded(c);
        boolean registered = username != null
                && userRepository.findByUsername(username)
                        .flatMap(u -> entryRepository.findByContestIdAndUserId(id, u.getId()))
                        .isPresent();
        List<ContestDetailDto.ProblemRef> problems = c.getProblems().stream()
                .map(p -> new ContestDetailDto.ProblemRef(p.getSlug(), p.getTitle(), p.getDifficulty()))
                .toList();
        return new ContestDetailDto(
                c.getId(), c.getName(), c.getDescription(), c.statusAt(Instant.now()),
                c.getStartTime(), c.getEndTime(), entryRepository.countByContestId(id), registered, problems);
    }

    @Transactional
    public void register(Long id, String username) {
        Contest c = requireContest(id);
        if (c.statusAt(Instant.now()) == Contest.Status.ENDED) {
            throw new IllegalStateException("Contest has already ended");
        }
        User user = userService.requireByUsername(username);
        if (user.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Admins can create contests but cannot participate in them.");
        }
        if (entryRepository.findByContestIdAndUserId(id, user.getId()).isEmpty()) {
            entryRepository.save(new ContestEntry(c, user));
            broadcastStandings(c);
        }
    }

    @Transactional(readOnly = false)
    public List<StandingRowDto> standings(Long id) {
        Contest c = requireContest(id);
        finalizeIfEnded(c);
        return buildStandings(c);
    }

    /**
     * Records a solve for any running contest that includes the problem and in
     * which the user is registered. Returns true if a standing changed.
     */
    @Transactional
    public boolean recordContestSolve(User user, Problem problem) {
        Instant now = Instant.now();
        boolean changed = false;
        for (ContestEntry entry : entryRepository.findByUserId(user.getId())) {
            Contest c = entry.getContest();
            if (c.statusAt(now) != Contest.Status.RUNNING) {
                continue;
            }
            boolean inContest = c.getProblems().stream().anyMatch(p -> p.getId().equals(problem.getId()));
            if (!inContest || entry.getSolvedProblemIds().contains(problem.getId())) {
                continue;
            }
            entry.getSolvedProblemIds().add(problem.getId());
            long minutes = Math.max(0, Duration.between(c.getStartTime(), now).toMinutes());
            entry.setPenaltyMinutes(entry.getPenaltyMinutes() + minutes);
            entryRepository.save(entry);
            broadcastStandings(c);
            changed = true;
        }
        return changed;
    }

    /** Applies Elo rating changes once, when a contest has ended. */
    private void finalizeIfEnded(Contest c) {
        if (c.statusAt(Instant.now()) != Contest.Status.ENDED || c.isRatingApplied()) {
            return;
        }
        List<ContestEntry> entries = entryRepository.findByContestId(c.getId());
        if (!entries.isEmpty()) {
            List<RatingService.Contestant> contestants = entries.stream()
                    .map(e -> new RatingService.Contestant(
                            e.getId(), e.getUser().getRating(), e.solvedCount(), e.getPenaltyMinutes()))
                    .toList();
            Map<Long, Integer> deltas = ratingService.computeContestDeltas(contestants).stream()
                    .collect(Collectors.toMap(RatingService.Delta::entryId, RatingService.Delta::delta));
            for (ContestEntry e : entries) {
                int before = e.getUser().getRating();
                int after = Math.max(RatingService.MIN_RATING, before + deltas.getOrDefault(e.getId(), 0));
                e.setRatingBefore(before);
                e.setRatingAfter(after);
                e.getUser().setRating(after);
                userRepository.save(e.getUser());
                entryRepository.save(e);
            }
            userService.broadcastLeaderboard();
        }
        c.setRatingApplied(true);
        contestRepository.save(c);
    }

    private List<StandingRowDto> buildStandings(Contest c) {
        Map<Long, String> idToSlug = c.getProblems().stream()
                .collect(Collectors.toMap(Problem::getId, Problem::getSlug, (a, b) -> a));
        List<ContestEntry> entries = entryRepository.findByContestId(c.getId());
        entries.sort(Comparator
                .comparingInt(ContestEntry::solvedCount).reversed()
                .thenComparingLong(ContestEntry::getPenaltyMinutes)
                .thenComparing(e -> e.getUser().getUsername()));

        int rank = 1;
        List<StandingRowDto> rows = new java.util.ArrayList<>(entries.size());
        for (ContestEntry e : entries) {
            List<String> slugs = e.getSolvedProblemIds().stream()
                    .map(idToSlug::get)
                    .filter(java.util.Objects::nonNull)
                    .sorted()
                    .toList();
            Integer delta = (e.getRatingBefore() != null && e.getRatingAfter() != null)
                    ? e.getRatingAfter() - e.getRatingBefore() : null;
            rows.add(new StandingRowDto(
                    rank++, e.getUser().getUsername(), e.getUser().getRating(),
                    e.solvedCount(), e.getPenaltyMinutes(), delta, slugs));
        }
        return rows;
    }

    /** True if the user is registered in a RUNNING contest that includes the problem. */
    @Transactional(readOnly = true)
    public boolean inActiveContest(String username, Long problemId) {
        if (username == null || username.isBlank()) {
            return false;
        }
        Instant now = Instant.now();
        return userRepository.findByUsername(username)
                .map(u -> entryRepository.findByUserId(u.getId()).stream().anyMatch(e -> {
                    Contest c = e.getContest();
                    return c.statusAt(now) == Contest.Status.RUNNING
                            && c.getProblems().stream().anyMatch(p -> p.getId().equals(problemId));
                }))
                .orElse(false);
    }

    /** A user's contest participation history (most recent first). */
    @Transactional(readOnly = true)
    public List<ContestHistoryDto> historyFor(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        Instant now = Instant.now();
        return entryRepository.findByUserId(user.getId()).stream()
                .map(e -> {
                    Contest c = e.getContest();
                    Integer delta = (e.getRatingBefore() != null && e.getRatingAfter() != null)
                            ? e.getRatingAfter() - e.getRatingBefore() : null;
                    return new ContestHistoryDto(
                            c.getId(), c.getName(), c.statusAt(now), c.getStartTime(), c.getEndTime(),
                            e.solvedCount(), e.getPenaltyMinutes(),
                            e.getRatingBefore(), e.getRatingAfter(), delta);
                })
                .sorted((a, b) -> b.startTime().compareTo(a.startTime()))
                .toList();
    }

    private void broadcastStandings(Contest c) {
        sseHub.broadcast("contest-" + c.getId(), "update", buildStandings(c));
    }

    private Contest requireContest(Long id) {
        return contestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contest not found: " + id));
    }
}
