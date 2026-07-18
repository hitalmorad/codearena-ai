package com.codearena.service;

import com.codearena.dto.LeaderboardEntryDto;
import com.codearena.model.Problem;
import com.codearena.model.Role;
import com.codearena.model.User;
import com.codearena.realtime.SseHub;
import com.codearena.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RatingService ratingService;
    private final SseHub sseHub;

    public UserService(UserRepository userRepository, RatingService ratingService, SseHub sseHub) {
        this.userRepository = userRepository;
        this.ratingService = ratingService;
        this.sseHub = sseHub;
    }

    /** Register a username, or return the existing account (login-or-create). */
    @Transactional
    public User registerOrGet(String username) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User created = userRepository.save(new User(username));
                    broadcastLeaderboard();
                    return created;
                });
    }

    @Transactional(readOnly = true)
    public User requireByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    public java.util.Optional<User> find(String username) {
        return username == null ? java.util.Optional.empty() : userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> leaderboard() {
        // Admins create content but never compete, so they are excluded here.
        List<User> users = userRepository.findTop100ByRoleOrderByScoreDescProblemsSolvedDescUsernameAsc(Role.USER);
        List<LeaderboardEntryDto> rows = new ArrayList<>(users.size());
        int rank = 1;
        for (User u : users) {
            rows.add(new LeaderboardEntryDto(rank++, u.getUsername(), u.getRating(), u.getProblemsSolved()));
        }
        return rows;
    }

    /**
     * Records a first-time solve. Solving grants a hidden problem-solving score
     * (weighted by difficulty) used for the global leaderboard — it does NOT
     * change the user's rating. Rating only changes from rated contests.
     * Returns true if this is the user's first accepted solve of the problem.
     */
    @Transactional
    public boolean applyGlobalSolve(User user, Problem problem) {
        if (user.getSolvedProblemIds().contains(problem.getId())) {
            return false;
        }
        user.getSolvedProblemIds().add(problem.getId());
        user.setProblemsSolved(user.getProblemsSolved() + 1);
        user.setScore(user.getScore() + ratingService.solveReward(problem.getDifficulty()));
        userRepository.save(user);
        return true;
    }

    public void broadcastLeaderboard() {
        sseHub.broadcast("leaderboard", "update", leaderboard());
    }
}
