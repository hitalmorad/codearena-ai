package com.codearena.config;

import com.codearena.judge.Starters;
import com.codearena.model.Contest;
import com.codearena.model.Difficulty;
import com.codearena.model.Problem;
import com.codearena.model.Role;
import com.codearena.model.TestCase;
import com.codearena.model.User;
import com.codearena.repository.ContestRepository;
import com.codearena.repository.ProblemRepository;
import com.codearena.repository.UserRepository;
import com.codearena.security.PasswordHasher;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds a LeetCode-style problem set, a few demo competitors and a running
 * contest on first boot. All problems use a simple stdin/stdout I/O contract
 * and ship starter code for every supported language.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final ContestRepository contestRepository;
    private final String adminPassword;

    public DataSeeder(ProblemRepository problemRepository,
                      UserRepository userRepository,
                      ContestRepository contestRepository,
                      @Value("${codearena.admin.password:admin123}") String adminPassword) {
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
        this.contestRepository = contestRepository;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (problemRepository.count() > 0) {
            return;
        }
        seedProblems();
        seedUsers();
        seedContest();
    }

    private void seedProblems() {

        seed("sum-of-two-numbers", "Sum of Two Numbers", Difficulty.EASY,
                List.of("math", "implementation"),
                """
                Read two space-separated integers `a` and `b` and print their sum.

                ### Input
                A single line with two integers `a` and `b` (-10^9 ≤ a, b ≤ 10^9).

                ### Output
                A single integer — the value of `a + b`.

                ### Example
                Input:  `2 3`
                Output: `5`
                """,
                List.of(s("2 3", "5"), s("10 20", "30"),
                        h("-5 5", "0"), h("1000000000 1000000000", "2000000000")));

        seed("reverse-a-string", "Reverse a String", Difficulty.EASY,
                List.of("strings"),
                """
                Read a single line of text and print it reversed.

                ### Input
                One line containing a string `s` (1 ≤ |s| ≤ 1000).

                ### Output
                The string `s` reversed.

                ### Example
                Input:  `hello`
                Output: `olleh`
                """,
                List.of(s("hello", "olleh"), s("CodeArena", "anerAedoC"),
                        h("racecar", "racecar"), h("a", "a")));

        seed("fizzbuzz", "FizzBuzz", Difficulty.EASY,
                List.of("implementation", "math"),
                """
                For each number from 1 to `n` print, on its own line: `Fizz` if
                divisible by 3, `Buzz` if divisible by 5, `FizzBuzz` if divisible by
                both, otherwise the number itself.

                ### Input
                A single integer `n` (1 ≤ n ≤ 10000).

                ### Output
                `n` lines following the rules above.

                ### Example
                Input:  `5`
                Output:
                ```
                1
                2
                Fizz
                4
                Buzz
                ```
                """,
                List.of(s("5", "1\n2\nFizz\n4\nBuzz"),
                        h("15", "1\n2\nFizz\n4\nBuzz\nFizz\n7\n8\nFizz\nBuzz\n11\nFizz\n13\n14\nFizzBuzz"),
                        h("3", "1\n2\nFizz")));

        seed("maximum-of-array", "Maximum of Array", Difficulty.EASY,
                List.of("arrays"),
                """
                Find the largest element of an array.

                ### Input
                The first line contains an integer `n` (1 ≤ n ≤ 10^5).
                The second line contains `n` integers.

                ### Output
                The maximum element.

                ### Example
                Input:
                ```
                5
                3 7 2 9 4
                ```
                Output: `9`
                """,
                List.of(s("5\n3 7 2 9 4", "9"), h("1\n-5", "-5"), h("3\n10 10 10", "10")));

        seed("factorial", "Factorial", Difficulty.EASY,
                List.of("math"),
                """
                Compute `n!` (the product of all integers from 1 to `n`).

                ### Input
                A single integer `n` (0 ≤ n ≤ 20).

                ### Output
                The value of `n!`. Note that `0! = 1`.

                ### Example
                Input:  `5`
                Output: `120`
                """,
                List.of(s("5", "120"), s("0", "1"), h("10", "3628800"), h("20", "2432902008176640000")));

        seed("nth-fibonacci", "Nth Fibonacci Number", Difficulty.EASY,
                List.of("math", "dynamic-programming"),
                """
                The Fibonacci sequence is defined by `F(0) = 0`, `F(1) = 1`, and
                `F(n) = F(n-1) + F(n-2)`. Print `F(n)`.

                ### Input
                A single integer `n` (0 ≤ n ≤ 90).

                ### Output
                The value of `F(n)`.

                ### Example
                Input:  `10`
                Output: `55`
                """,
                List.of(s("10", "55"), s("0", "0"), h("1", "1"), h("20", "6765")));

        seed("count-vowels", "Count the Vowels", Difficulty.EASY,
                List.of("strings"),
                """
                Count how many vowels (`a`, `e`, `i`, `o`, `u`, case-insensitive) a
                string contains.

                ### Input
                One line containing a string `s` (1 ≤ |s| ≤ 1000).

                ### Output
                The number of vowels in `s`.

                ### Example
                Input:  `hello`
                Output: `2`
                """,
                List.of(s("hello", "2"), s("AEIOU", "5"), h("xyz", "0"), h("CodeArena", "4")));

        seed("palindrome-number", "Palindrome Number", Difficulty.EASY,
                List.of("math"),
                """
                Determine whether an integer reads the same forwards and backwards.

                ### Input
                A single non-negative integer `n` (0 ≤ n ≤ 10^9).

                ### Output
                `YES` if `n` is a palindrome, otherwise `NO`.

                ### Example
                Input:  `121`
                Output: `YES`
                """,
                List.of(s("121", "YES"), s("123", "NO"), h("10", "NO"), h("7", "YES")));

        seed("gcd-of-two-numbers", "Greatest Common Divisor", Difficulty.EASY,
                List.of("math", "number-theory"),
                """
                Compute the greatest common divisor of two positive integers.

                ### Input
                A single line with two integers `a` and `b` (1 ≤ a, b ≤ 10^9).

                ### Output
                The GCD of `a` and `b`.

                ### Example
                Input:  `12 18`
                Output: `6`
                """,
                List.of(s("12 18", "6"), s("17 5", "1"), h("100 10", "10"), h("270 192", "6")));

        seed("prime-check", "Prime Check", Difficulty.MEDIUM,
                List.of("math", "number-theory"),
                """
                Determine whether a number is prime.

                ### Input
                A single integer `n` (1 ≤ n ≤ 10^9).

                ### Output
                `YES` if `n` is prime, otherwise `NO`.

                ### Example
                Input:  `7`
                Output: `YES`
                """,
                List.of(s("7", "YES"), s("1", "NO"), h("2", "YES"), h("100", "NO"), h("97", "YES")));

        seed("sort-the-array", "Sort the Array", Difficulty.MEDIUM,
                List.of("arrays", "sorting"),
                """
                Sort an array of integers in non-decreasing order.

                ### Input
                The first line contains an integer `n` (1 ≤ n ≤ 10^5).
                The second line contains `n` integers.

                ### Output
                The `n` integers sorted ascending, separated by single spaces.

                ### Example
                Input:
                ```
                5
                5 3 8 1 2
                ```
                Output: `1 2 3 5 8`
                """,
                List.of(s("5\n5 3 8 1 2", "1 2 3 5 8"), h("1\n42", "42"), h("3\n-1 -5 0", "-5 -1 0")));

        seed("second-largest", "Second Largest Element", Difficulty.MEDIUM,
                List.of("arrays"),
                """
                Find the second largest **distinct** value in an array. It is
                guaranteed the array has at least two distinct values.

                ### Input
                The first line contains an integer `n` (2 ≤ n ≤ 10^5).
                The second line contains `n` integers.

                ### Output
                The second largest distinct value.

                ### Example
                Input:
                ```
                5
                3 7 2 9 4
                ```
                Output: `7`
                """,
                List.of(s("5\n3 7 2 9 4", "7"), h("6\n10 10 9 8 8 7", "9"), h("2\n1 2", "1")));
    }

    private void seedUsers() {
        createUser("alice", 1485, 9);
        createUser("bob", 1360, 6);
        createUser("carol", 1240, 3);
        createUser("dave", 1180, 1);

        // Admin account (username: admin, password: from config, role: ADMIN)
        User admin = new User("admin");
        admin.setPasswordHash(PasswordHasher.hash(adminPassword));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
    }

    private void createUser(String username, int rating, int solved) {
        User u = new User(username);
        // Demo accounts share the password "password" so they can be logged into.
        u.setPasswordHash(PasswordHasher.hash("password"));
        u.setRole(Role.USER);
        u.setRating(rating);
        u.setProblemsSolved(solved);
        userRepository.save(u);
    }

    private void seedContest() {
        Contest contest = new Contest();
        contest.setName("Weekly Challenge #1");
        contest.setDescription("""
                A beginner-friendly warm-up round. Solve as many problems as you can
                before the timer runs out — every solve improves your rating!
                """);
        Instant now = Instant.now();
        contest.setStartTime(now.minus(5, ChronoUnit.MINUTES));
        contest.setEndTime(now.plus(3, ChronoUnit.HOURS));
        contest.setProblems(List.of(
                problemRepository.findBySlug("sum-of-two-numbers").orElseThrow(),
                problemRepository.findBySlug("reverse-a-string").orElseThrow(),
                problemRepository.findBySlug("fizzbuzz").orElseThrow(),
                problemRepository.findBySlug("prime-check").orElseThrow(),
                problemRepository.findBySlug("gcd-of-two-numbers").orElseThrow()));
        contestRepository.save(contest);
    }

    private void seed(String slug, String title, Difficulty difficulty,
                      List<String> tags, String description, List<TestCase> tests) {
        Problem p = new Problem();
        p.setSlug(slug);
        p.setTitle(title);
        p.setDifficulty(difficulty);
        p.setTags(tags);
        p.setDescription(description);
        p.setStarterCode(Starters.defaults());
        tests.forEach(p::addTestCase);
        problemRepository.save(p);
    }

    /** Sample (visible) test case. */
    private TestCase s(String input, String output) {
        return new TestCase(input, output, true);
    }

    /** Hidden test case. */
    private TestCase h(String input, String output) {
        return new TestCase(input, output, false);
    }
}
