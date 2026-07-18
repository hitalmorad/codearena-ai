package com.codearena.service;

import com.codearena.dto.AuthResponse;
import com.codearena.model.Role;
import com.codearena.model.User;
import com.codearena.repository.UserRepository;
import com.codearena.security.JwtService;
import com.codearena.security.PasswordHasher;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtService jwtService;
    /** Lower-cased set of emails that automatically receive the ADMIN role. */
    private final Set<String> adminEmails;

    public AuthService(UserRepository userRepository, UserService userService, JwtService jwtService,
                       @Value("${codearena.admin.emails:}") String adminEmailsCsv) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.jwtService = jwtService;
        this.adminEmails = Arrays.stream(adminEmailsCsv.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Whether the given email is on the admin allowlist. */
    public boolean isAdminEmail(String email) {
        return email != null && adminEmails.contains(email.trim().toLowerCase(Locale.ROOT));
    }

    /** Builds the auth payload with a freshly issued JWT. */
    public AuthResponse issueResponse(User u) {
        return new AuthResponse(u.getUsername(), u.getEmail(), u.getRating(), u.getProblemsSolved(),
                u.getRole(), jwtService.issue(u));
    }

    @Transactional
    public User register(String username, String email, String password) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsername(username)) {
            throw new IllegalStateException("Username '" + username + "' is already taken");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("An account with that email already exists");
        }
        User u = new User(username);
        u.setEmail(normalizedEmail);
        u.setPasswordHash(PasswordHasher.hash(password));
        u.setRole(isAdminEmail(normalizedEmail) ? Role.ADMIN : Role.USER);
        User saved = userRepository.save(u);
        userService.broadcastLeaderboard();
        return saved;
    }

    @Transactional
    public User login(String username, String password) {
        User u = userRepository.findByUsername(username)
                .filter(user -> user.getPasswordHash() != null
                        && PasswordHasher.verify(password, user.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid username or password"));
        // Grant admin automatically if the account's email is on the allowlist.
        if (isAdminEmail(u.getEmail()) && u.getRole() != Role.ADMIN) {
            u.setRole(Role.ADMIN);
            u = userRepository.save(u);
        }
        return u;
    }

    /** Resolves the user behind a JWT, validating signature, expiry and token version. */
    @Transactional(readOnly = true)
    public Optional<User> resolveToken(String token) {
        return jwtService.parse(token).flatMap(claims -> {
            User u = userRepository.findByUsername(claims.getSubject()).orElse(null);
            if (u == null) {
                return Optional.empty();
            }
            Integer ver = claims.get("ver", Integer.class);
            if (ver == null || ver != u.getTokenVersion()) {
                return Optional.empty();
            }
            return Optional.of(u);
        });
    }

    /** Resolves a user from a token or fails with 401. */
    @Transactional(readOnly = true)
    public User requireToken(String token) {
        return resolveToken(token).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }

    @Transactional
    public User updateBio(String token, String bio) {
        User u = requireToken(token);
        u.setBio(bio == null ? null : bio.strip());
        return userRepository.save(u);
    }

    @Transactional
    public User changePassword(String token, String currentPassword, String newPassword) {
        User u = requireToken(token);
        if (u.getPasswordHash() == null || !PasswordHasher.verify(currentPassword, u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        u.setPasswordHash(PasswordHasher.hash(newPassword));
        u.setTokenVersion(u.getTokenVersion() + 1); // invalidate all existing tokens
        return userRepository.save(u);
    }

    /** Resolves the user from a token, requiring the ADMIN role. */
    @Transactional(readOnly = true)
    public boolean isAdminToken(String token) {
        return resolveToken(token).map(u -> u.getRole() == Role.ADMIN).orElse(false);
    }
}
