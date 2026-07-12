package com.codearena.judge;

/**
 * Normalizes and compares program output against expected output, ignoring
 * trailing whitespace on each line and trailing blank lines.
 */
public final class OutputMatcher {

    private OutputMatcher() {
    }

    public static boolean matches(String expected, String actual) {
        return normalize(expected).equals(normalize(actual));
    }

    public static String normalize(String s) {
        if (s == null) {
            return "";
        }
        String[] lines = s.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(stripTrailing(line)).append('\n');
        }
        String out = sb.toString();
        int end = out.length();
        while (end > 0 && out.charAt(end - 1) == '\n') {
            end--;
        }
        return out.substring(0, end);
    }

    private static String stripTrailing(String s) {
        int end = s.length();
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }
        return s.substring(0, end);
    }
}
