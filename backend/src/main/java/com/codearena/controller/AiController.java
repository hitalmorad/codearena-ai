package com.codearena.controller;

import com.codearena.ai.AiRateLimiter;
import com.codearena.ai.AiService;
import com.codearena.dto.HintRequest;
import com.codearena.dto.HintResponse;
import com.codearena.dto.InterviewRequest;
import com.codearena.dto.InterviewResponse;
import com.codearena.model.User;
import com.codearena.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * AI endpoints: Socratic hints and a mock interviewer. Both require a valid
 * JWT and are rate-limited per user.
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final AuthService authService;
    private final AiRateLimiter rateLimiter;

    public AiController(AiService aiService, AuthService authService, AiRateLimiter rateLimiter) {
        this.aiService = aiService;
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    /** Public: lets the UI show whether the AI provider is active. */
    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("enabled", aiService.aiEnabled());
    }

    @PostMapping("/hint")
    public HintResponse hint(@RequestHeader(value = "X-Auth-Token", required = false) String token,
                             @Valid @RequestBody HintRequest request) {
        User u = requireUser(token);
        return aiService.hint(request.slug(), request.level(), request.language(), request.sourceCode());
    }

    @PostMapping("/interview")
    public InterviewResponse interview(@RequestHeader(value = "X-Auth-Token", required = false) String token,
                                       @Valid @RequestBody InterviewRequest request) {
        User u = requireUser(token);
        return aiService.interview(request.slug(), request.history(), request.message());
    }

    private User requireUser(String token) {
        User u = authService.requireToken(token);
        if (!rateLimiter.allow(u.getUsername())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many AI requests — please slow down.");
        }
        return u;
    }
}
