package com.codearena.controller;

import com.codearena.dto.ActivityDto;
import com.codearena.dto.ContestHistoryDto;
import com.codearena.dto.LeaderboardEntryDto;
import com.codearena.dto.ProfileDto;
import com.codearena.dto.RegisterRequest;
import com.codearena.dto.SubmissionResponse;
import com.codearena.dto.UserDto;
import com.codearena.realtime.SseHub;
import com.codearena.service.ContestService;
import com.codearena.service.ProfileService;
import com.codearena.service.SubmissionService;
import com.codearena.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final SubmissionService submissionService;
    private final ContestService contestService;
    private final ProfileService profileService;
    private final SseHub sseHub;

    public UserController(UserService userService,
                          SubmissionService submissionService,
                          ContestService contestService,
                          ProfileService profileService,
                          SseHub sseHub) {
        this.userService = userService;
        this.submissionService = submissionService;
        this.contestService = contestService;
        this.profileService = profileService;
        this.sseHub = sseHub;
    }

    @PostMapping("/users/register")
    public UserDto register(@Valid @RequestBody RegisterRequest request) {
        return UserDto.from(userService.registerOrGet(request.username()));
    }

    @GetMapping("/users/{username}")
    public UserDto get(@PathVariable String username) {
        return UserDto.from(userService.requireByUsername(username));
    }

    @GetMapping("/users/{username}/profile")
    public ProfileDto profile(@PathVariable String username) {
        return profileService.build(username);
    }

    @GetMapping("/users/{username}/activity")
    public List<ActivityDto> activity(@PathVariable String username) {
        return profileService.activity(username);
    }

    @GetMapping("/users/{username}/submissions")
    public List<SubmissionResponse> submissions(@PathVariable String username) {
        return submissionService.historyFor(username);
    }

    @GetMapping("/users/{username}/contests")
    public List<ContestHistoryDto> contests(@PathVariable String username) {
        return contestService.historyFor(username);
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardEntryDto> leaderboard() {
        return userService.leaderboard();
    }

    @GetMapping("/leaderboard/stream")
    public SseEmitter leaderboardStream() {
        return sseHub.subscribe("leaderboard");
    }
}
