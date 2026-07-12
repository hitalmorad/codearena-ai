package com.codearena.model;

/**
 * Judge verdicts. Mirrors the classic competitive-programming result set.
 */
public enum Verdict {
    PENDING,
    ACCEPTED,
    WRONG_ANSWER,
    TIME_LIMIT_EXCEEDED,
    MEMORY_LIMIT_EXCEEDED,
    RUNTIME_ERROR,
    COMPILATION_ERROR,
    INTERNAL_ERROR
}
