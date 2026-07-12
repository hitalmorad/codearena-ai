package com.codearena.judge;

import com.codearena.model.Language;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic per-language starter code templates. Problems use a simple
 * stdin/stdout I/O contract, so a single skeleton works for every problem — the
 * user reads input and prints the answer according to the problem statement.
 */
public final class Starters {

    private Starters() {
    }

    public static Map<Language, String> defaults() {
        Map<Language, String> m = new LinkedHashMap<>();
        m.put(Language.PYTHON, """
                import sys

                data = sys.stdin.read().split()
                # TODO: read the input and print your answer
                """);
        m.put(Language.JAVASCRIPT, """
                const input = require('fs').readFileSync(0, 'utf8').trim();
                const data = input.split(/\\s+/);
                // TODO: read the input and print your answer
                """);
        m.put(Language.JAVA, """
                import java.util.*;
                import java.io.*;

                public class Main {
                    public static void main(String[] args) throws IOException {
                        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                        // TODO: read the input and print your answer
                    }
                }
                """);
        m.put(Language.CPP, """
                #include <bits/stdc++.h>
                using namespace std;

                int main() {
                    ios::sync_with_stdio(false);
                    cin.tie(nullptr);
                    // TODO: read the input and print your answer
                    return 0;
                }
                """);
        m.put(Language.C, """
                #include <stdio.h>

                int main(void) {
                    /* TODO: read the input and print your answer */
                    return 0;
                }
                """);
        m.put(Language.GO, """
                package main

                import (
                    "bufio"
                    "fmt"
                    "os"
                )

                func main() {
                    reader := bufio.NewReader(os.Stdin)
                    _ = reader
                    _ = fmt.Sprint
                    // TODO: read the input and print your answer
                }
                """);
        return m;
    }
}
