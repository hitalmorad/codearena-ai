package com.codearena.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codearena.ai.AiRateLimiter;
import com.codearena.ai.AiService;
import com.codearena.dto.HintResponse;
import com.codearena.dto.InterviewResponse;
import com.codearena.model.User;
import com.codearena.service.AuthService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

/**
 * Web-layer tests for {@link AiController} using a standalone MockMvc setup so
 * they run without booting the full Spring context (fast + deterministic).
 */
class AiControllerTest {

    private final AiService aiService = mock(AiService.class);
    private final AuthService authService = mock(AuthService.class);
    private final AiRateLimiter rateLimiter = mock(AiRateLimiter.class);

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        AiController controller = new AiController(aiService, authService, rateLimiter);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private User user() {
        return new User("alice");
    }

    @Test
    void status_isPublicAndReportsDisabled() throws Exception {
        when(aiService.aiEnabled()).thenReturn(false);
        mvc.perform(get("/api/ai/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void hint_withoutToken_returns401() throws Exception {
        when(authService.requireToken(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));

        mvc.perform(post("/api/ai/hint")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"two-sum\",\"level\":1,\"language\":\"PYTHON\",\"sourceCode\":\"\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void hint_blankSlug_returns400() throws Exception {
        mvc.perform(post("/api/ai/hint")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"\",\"level\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hint_happyPath_returnsHint() throws Exception {
        when(authService.requireToken(any())).thenReturn(user());
        when(rateLimiter.allow(anyString())).thenReturn(true);
        when(aiService.hint(anyString(), anyInt(), any(), any()))
                .thenReturn(new HintResponse(1, "Try a hash map.", "heuristic"));

        mvc.perform(post("/api/ai/hint")
                        .header("X-Auth-Token", "valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"two-sum\",\"level\":1,\"language\":\"PYTHON\",\"sourceCode\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.text").value("Try a hash map."))
                .andExpect(jsonPath("$.source").value("heuristic"));
    }

    @Test
    void hint_overRateLimit_returns429() throws Exception {
        when(authService.requireToken(any())).thenReturn(user());
        when(rateLimiter.allow(anyString())).thenReturn(false);

        mvc.perform(post("/api/ai/hint")
                        .header("X-Auth-Token", "valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"two-sum\",\"level\":1,\"language\":\"PYTHON\",\"sourceCode\":\"\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void interview_blankMessage_returns400() throws Exception {
        mvc.perform(post("/api/ai/interview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":null,\"history\":[],\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void interview_happyPath_returnsReply() throws Exception {
        when(authService.requireToken(any())).thenReturn(user());
        when(rateLimiter.allow(anyString())).thenReturn(true);
        when(aiService.interview(any(), any(), anyString()))
                .thenReturn(new InterviewResponse("What is your approach?", List.of(), "heuristic"));

        mvc.perform(post("/api/ai/interview")
                        .header("X-Auth-Token", "valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":null,\"history\":[],\"message\":\"I am ready\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("What is your approach?"))
                .andExpect(jsonPath("$.source").value("heuristic"));
    }
}
