package com.codearena.ai;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Simple in-memory sliding-window rate limiter, keyed by username. Protects the
 * Groq quota and local resources from runaway AI requests.
 */
@Component
public class AiRateLimiter {

    private static final int MAX_PER_WINDOW = 30;
    private static final long WINDOW_MS = 60_000L;

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public boolean allow(String username) {
        Deque<Long> dq = hits.computeIfAbsent(username, k -> new ArrayDeque<>());
        long now = System.currentTimeMillis();
        synchronized (dq) {
            while (!dq.isEmpty() && now - dq.peekFirst() > WINDOW_MS) {
                dq.pollFirst();
            }
            if (dq.size() >= MAX_PER_WINDOW) {
                return false;
            }
            dq.addLast(now);
            return true;
        }
    }
}
