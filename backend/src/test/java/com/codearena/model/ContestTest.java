package com.codearena.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codearena.model.Contest.Status;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class ContestTest {

    private Contest contest(Instant start, Instant end) {
        Contest c = new Contest();
        c.setName("Weekly");
        c.setStartTime(start);
        c.setEndTime(end);
        return c;
    }

    @Test
    void statusAt_beforeStart_isUpcoming() {
        Instant now = Instant.now();
        Contest c = contest(now.plus(1, ChronoUnit.HOURS), now.plus(2, ChronoUnit.HOURS));
        assertEquals(Status.UPCOMING, c.statusAt(now));
    }

    @Test
    void statusAt_withinWindow_isRunning() {
        Instant now = Instant.now();
        Contest c = contest(now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));
        assertEquals(Status.RUNNING, c.statusAt(now));
    }

    @Test
    void statusAt_afterEnd_isEnded() {
        Instant now = Instant.now();
        Contest c = contest(now.minus(2, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS));
        assertEquals(Status.ENDED, c.statusAt(now));
    }

    @Test
    void contestEntry_solvedCountReflectsSolvedIds() {
        ContestEntry entry = new ContestEntry(new Contest(), new User("alice"));
        assertEquals(0, entry.solvedCount());
        entry.getSolvedProblemIds().add(1L);
        entry.getSolvedProblemIds().add(2L);
        entry.getSolvedProblemIds().add(2L); // duplicate ignored by the set
        assertEquals(2, entry.solvedCount());
    }
}
