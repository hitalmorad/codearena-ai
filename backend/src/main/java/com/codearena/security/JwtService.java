package com.codearena.security;

import com.codearena.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Issues and validates stateless HS256 JSON Web Tokens.
 *
 * <p>A token carries the username (subject), the user's role and a
 * {@code ver} claim that mirrors {@link User#getTokenVersion()}. Bumping the
 * user's token version therefore invalidates every previously issued token
 * (used on password change), which is our lightweight revocation mechanism.
 */
@Component
public class JwtService {

    private final SecretKey key;
    private final long ttlMillis;

    public JwtService(@Value("${codearena.jwt.secret}") String secret,
                      @Value("${codearena.jwt.ttl-hours}") long ttlHours) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "codearena.jwt.secret must be at least 32 bytes for HS256");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.ttlMillis = ttlHours * 3_600_000L;
    }

    /** Issues a signed token for the given user. */
    public String issue(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole().name())
                .claim("ver", user.getTokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttlMillis, ChronoUnit.MILLIS)))
                .signWith(key)
                .compact();
    }

    /** Parses and validates a token, returning its claims if the signature and expiry are valid. */
    public Optional<Claims> parse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
