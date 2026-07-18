package com.codearena.judge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OutputMatcherTest {

    @Test
    void trailingWhitespaceOnLines_isIgnored() {
        assertTrue(OutputMatcher.matches("1 2 3", "1 2 3   "));
        assertTrue(OutputMatcher.matches("hello\tworld", "hello\tworld  "));
    }

    @Test
    void trailingBlankLines_areIgnored() {
        assertTrue(OutputMatcher.matches("a\nb", "a\nb\n\n\n"));
        assertTrue(OutputMatcher.matches("a\nb\n", "a\nb"));
    }

    @Test
    void windowsAndUnixLineEndings_areTreatedTheSame() {
        assertTrue(OutputMatcher.matches("a\r\nb\r\n", "a\nb"));
        assertTrue(OutputMatcher.matches("a\rb", "a\nb"));
    }

    @Test
    void nullAndEmpty_normalizeToTheSame() {
        assertTrue(OutputMatcher.matches(null, ""));
        assertTrue(OutputMatcher.matches(null, "\n\n"));
        assertTrue(OutputMatcher.matches("", null));
    }

    @Test
    void genuineDifferences_doNotMatch() {
        assertFalse(OutputMatcher.matches("hello", "world"));
        assertFalse(OutputMatcher.matches("1\n2\n3", "1\n2\n4"));
        assertFalse(OutputMatcher.matches("42", "4 2"), "internal whitespace still matters");
    }

    @Test
    void leadingWhitespace_isSignificant() {
        assertFalse(OutputMatcher.matches("x", "  x"), "leading spaces are not stripped");
    }

    @Test
    void normalize_collapsesTrailingWhitespaceAndBlankLines() {
        assertEquals("a\nb", OutputMatcher.normalize("a   \nb\n\n"));
        assertEquals("", OutputMatcher.normalize("\n\n  \n"));
    }
}
