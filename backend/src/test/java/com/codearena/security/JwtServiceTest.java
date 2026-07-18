package com.codearena.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codearena.model.Role;
import com.codearena.model.User;
import io.jsonwebtoken.Claims;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "0123456789012345678901234567890123456789"; // 40 bytes

    private User user(String name, Role role, int version) {
        User u = new User(name);
        u.setRole(role);
        u.setTokenVersion(version);
        return u;
    }

    @Test
    void issuedToken_parsesBackToItsClaims() {
        JwtService jwt = new JwtService(SECRET, 24);
        String token = jwt.issue(user("alice", Role.USER, 3));

        Optional<Claims> parsed = jwt.parse(token);

        assertTrue(parsed.isPresent());
        assertEquals("alice", parsed.get().getSubject());
        assertEquals("USER", parsed.get().get("role", String.class));
        assertEquals(3, parsed.get().get("ver", Integer.class));
    }

    @Test
    void expiredToken_isRejected() {
        JwtService jwt = new JwtService(SECRET, -1); // exp one hour in the past
        String token = jwt.issue(user("bob", Role.USER, 0));

        assertTrue(jwt.parse(token).isEmpty());
    }

    @Test
    void tamperedToken_isRejected() {
        JwtService jwt = new JwtService(SECRET, 24);
        String token = jwt.issue(user("carol", Role.USER, 0));
        String tampered = token.substring(0, token.length() - 2)
                + (token.endsWith("a") ? "b" : "a") + "c";

        assertTrue(jwt.parse(tampered).isEmpty());
    }

    @Test
    void tokenSignedWithADifferentSecret_isRejected() {
        JwtService issuer = new JwtService(SECRET, 24);
        JwtService verifier = new JwtService("abcdefabcdefabcdefabcdefabcdefabcdef1234", 24);

        String token = issuer.issue(user("dave", Role.ADMIN, 0));

        assertTrue(verifier.parse(token).isEmpty());
    }

    @Test
    void blankOrNullToken_isRejected() {
        JwtService jwt = new JwtService(SECRET, 24);
        assertTrue(jwt.parse(null).isEmpty());
        assertTrue(jwt.parse("   ").isEmpty());
        assertTrue(jwt.parse("not-a-jwt").isEmpty());
    }

    @Test
    void adminRoleClaimIsCarried() {
        JwtService jwt = new JwtService(SECRET, 24);
        Optional<Claims> parsed = jwt.parse(jwt.issue(user("root", Role.ADMIN, 1)));
        assertEquals("ADMIN", parsed.orElseThrow().get("role", String.class));
    }

    @Test
    void secretShorterThan32Bytes_isRejectedAtConstruction() {
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short", 24));
    }
}
