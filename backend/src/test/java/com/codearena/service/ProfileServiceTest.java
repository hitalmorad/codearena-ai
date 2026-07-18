package com.codearena.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.codearena.dto.ProfileDto;
import com.codearena.model.Difficulty;
import com.codearena.model.Problem;
import com.codearena.model.User;
import com.codearena.model.Verdict;
import com.codearena.repository.ContestRepository;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.SubmissionRepository;
import com.codearena.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProblemRepository problemRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private ContestRepository contestRepository;

    private ProfileService service;

    @BeforeEach
    void setUp() {
        service = new ProfileService(userRepository, problemRepository, submissionRepository, contestRepository);
    }

    private Problem withDifficulty(Difficulty d) {
        Problem p = new Problem();
        p.setDifficulty(d);
        return p;
    }

    @Test
    void build_aggregatesSolvedByDifficultyRankAndAcceptance() {
        User u = new User("alice");
        u.setId(7L);
        u.setRating(1500);
        u.setScore(100);
        u.setBio("hi there");
        u.setSolvedProblemIds(Set.of(1L, 2L, 3L));

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(u));
        when(problemRepository.findAllById(any())).thenReturn(List.of(
                withDifficulty(Difficulty.EASY),
                withDifficulty(Difficulty.EASY),
                withDifficulty(Difficulty.MEDIUM)));
        when(problemRepository.countByDifficulty(Difficulty.EASY)).thenReturn(10L);
        when(problemRepository.countByDifficulty(Difficulty.MEDIUM)).thenReturn(5L);
        when(problemRepository.countByDifficulty(Difficulty.HARD)).thenReturn(2L);
        when(submissionRepository.countByUserId(7L)).thenReturn(20L);
        when(submissionRepository.countByUserIdAndVerdict(7L, Verdict.ACCEPTED)).thenReturn(8L);
        when(userRepository.countByScoreGreaterThan(100)).thenReturn(4L);
        when(userRepository.count()).thenReturn(30L);

        ProfileDto dto = service.build("alice");

        assertEquals("alice", dto.username());
        assertEquals("hi there", dto.bio());
        assertEquals(1500, dto.rating());
        assertEquals(5, dto.rank(), "rank = usersScoredHigher + 1");
        assertEquals(30, dto.totalUsers());
        assertEquals(2, dto.solvedEasy());
        assertEquals(1, dto.solvedMedium());
        assertEquals(0, dto.solvedHard());
        assertEquals(10, dto.totalEasy());
        assertEquals(5, dto.totalMedium());
        assertEquals(2, dto.totalHard());
        assertEquals(20, dto.totalSubmissions());
        assertEquals(8, dto.acceptedSubmissions());
    }

    @Test
    void build_topRatedUserIsRankOne() {
        User u = new User("champ");
        u.setId(1L);
        u.setRating(3000);
        u.setScore(999);
        u.setSolvedProblemIds(Set.of());

        when(userRepository.findByUsername("champ")).thenReturn(Optional.of(u));
        when(problemRepository.findAllById(any())).thenReturn(List.of());
        when(problemRepository.countByDifficulty(any())).thenReturn(1L);
        when(submissionRepository.countByUserId(1L)).thenReturn(0L);
        when(submissionRepository.countByUserIdAndVerdict(eq(1L), any())).thenReturn(0L);
        when(userRepository.countByScoreGreaterThan(999)).thenReturn(0L);
        when(userRepository.count()).thenReturn(50L);

        assertEquals(1, service.build("champ").rank());
    }

    @Test
    void build_unknownUser_throwsNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.build("ghost"));
    }
}
