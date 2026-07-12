package com.codearena.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AiRateLimiterTest {

    private static final int MAX_PER_WINDOW = 30;

    @Test
    void allowsUpToTheLimitThenBlocks() {
        AiRateLimiter limiter = new AiRateLimiter();
        for (int i = 0; i < MAX_PER_WINDOW; i++) {
            assertTrue(limiter.allow("alice"), "request " + (i + 1) + " should be allowed");
        }
        assertFalse(limiter.allow("alice"), "the 31st request in the window must be blocked");
    }

    @Test
    void usersAreRateLimitedIndependently() {
        AiRateLimiter limiter = new AiRateLimiter();
        for (int i = 0; i < MAX_PER_WINDOW; i++) {
            limiter.allow("alice");
        }
        assertFalse(limiter.allow("alice"), "alice is over her limit");
        assertTrue(limiter.allow("bob"), "bob has his own independent budget");
    }

    @Test
    void firstRequestForANewUserIsAlwaysAllowed() {
        AiRateLimiter limiter = new AiRateLimiter();
        assertTrue(limiter.allow("newcomer"));
    }
}
