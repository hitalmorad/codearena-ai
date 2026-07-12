package com.codearena.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codearena.dto.ChatMessage;
import com.codearena.dto.HintResponse;
import com.codearena.dto.InterviewResponse;
import com.codearena.model.Difficulty;
import com.codearena.model.Problem;
import com.codearena.repository.ProblemRepository;
import com.codearena.service.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private GroqClient groq;
    @Mock
    private ProblemRepository problemRepository;

    private AiService service;

    @BeforeEach
    void setUp() {
        service = new AiService(groq, problemRepository, new HeuristicHintEngine());
    }

    private Problem sampleProblem() {
        Problem p = new Problem();
        p.setSlug("two-sum");
        p.setTitle("Two Sum");
        p.setDescription("Find two numbers that add up to a target.");
        p.setDifficulty(Difficulty.EASY);
        p.setTags(List.of("hashing"));
        p.setTimeLimitMs(2000);
        return p;
    }

    // ---------- hint ----------

    @Test
    void hint_unknownSlug_throwsNotFound() {
        when(problemRepository.findBySlug("nope")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.hint("nope", 1, "PYTHON", ""));
    }

    @Test
    void hint_groqDisabled_usesHeuristicAndNeverCallsGroq() {
        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(sampleProblem()));
        when(groq.isEnabled()).thenReturn(false);

        HintResponse res = service.hint("two-sum", 1, "PYTHON", "");

        assertEquals("heuristic", res.source());
        assertFalse(res.text().isBlank());
        verify(groq, never()).chat(anyString(), any(), anyDouble(), anyInt());
    }

    @Test
    void hint_groqEnabled_returnsAiText() {
        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(sampleProblem()));
        when(groq.isEnabled()).thenReturn(true);
        when(groq.chat(anyString(), any(), anyDouble(), anyInt())).thenReturn("Consider a hash map.");

        HintResponse res = service.hint("two-sum", 2, "PYTHON", "print(1)");

        assertEquals("groq", res.source());
        assertEquals("Consider a hash map.", res.text());
        assertEquals(2, res.level());
    }

    @Test
    void hint_groqThrows_fallsBackToHeuristic() {
        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(sampleProblem()));
        when(groq.isEnabled()).thenReturn(true);
        when(groq.chat(anyString(), any(), anyDouble(), anyInt()))
                .thenThrow(new RuntimeException("network down"));

        HintResponse res = service.hint("two-sum", 1, "PYTHON", "");

        assertEquals("heuristic", res.source());
        assertFalse(res.text().isBlank());
    }

    @Test
    void hint_groqReturnsBlank_fallsBackToHeuristic() {
        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(sampleProblem()));
        when(groq.isEnabled()).thenReturn(true);
        when(groq.chat(anyString(), any(), anyDouble(), anyInt())).thenReturn("   ");

        HintResponse res = service.hint("two-sum", 1, "PYTHON", "");

        assertEquals("heuristic", res.source());
    }

    @Test
    void hint_levelIsClampedIntoOneToThree() {
        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(sampleProblem()));
        when(groq.isEnabled()).thenReturn(false);

        assertEquals(1, service.hint("two-sum", 0, "PYTHON", "").level());
        assertEquals(1, service.hint("two-sum", -9, "PYTHON", "").level());
        assertEquals(3, service.hint("two-sum", 7, "PYTHON", "").level());
    }

    // ---------- interview ----------

    @Test
    void interview_groqDisabled_returnsHeuristicAndAppendsBothTurns() {
        when(groq.isEnabled()).thenReturn(false);

        InterviewResponse res = service.interview(null, new ArrayList<>(), "I'm ready.");

        assertEquals("heuristic", res.source());
        assertFalse(res.reply().isBlank());
        assertEquals(2, res.history().size());
        assertEquals("user", res.history().get(0).role());
        assertEquals("I'm ready.", res.history().get(0).content());
        assertEquals("assistant", res.history().get(1).role());
    }

    @Test
    void interview_unknownSlug_isTreatedAsGeneralWithoutThrowing() {
        when(groq.isEnabled()).thenReturn(false);
        when(problemRepository.findBySlug("ghost")).thenReturn(Optional.empty());

        InterviewResponse res = service.interview("ghost", new ArrayList<>(), "hello");

        assertEquals("heuristic", res.source());
        assertEquals(2, res.history().size());
    }

    @Test
    void interview_longHistoryIsTrimmedBeforeSendingToGroq() {
        when(groq.isEnabled()).thenReturn(true);
        when(groq.chat(anyString(), any(), anyDouble(), anyInt())).thenReturn("Good, continue.");

        List<ChatMessage> history = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            history.add(new ChatMessage(i % 2 == 0 ? "user" : "assistant", "msg " + i));
        }

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatMessage>> captor = ArgumentCaptor.forClass(List.class);

        InterviewResponse res = service.interview("two-sum", history, "new message");

        verify(groq).chat(anyString(), captor.capture(), anyDouble(), anyInt());
        // trimmed to last 12 history turns + the 1 new user message
        assertEquals(13, captor.getValue().size());
        assertEquals("new message", captor.getValue().get(12).content());
        assertEquals("groq", res.source());
    }
}
