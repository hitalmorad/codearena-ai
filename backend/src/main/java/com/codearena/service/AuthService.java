package com.codearena.service;

import com.codearena.dto.AuthResponse;
import com.codearena.model.Role;
import com.codearena.model.User;
import com.codearena.repository.UserRepository;
import com.codearena.security.JwtService;
import com.codearena.security.PasswordHasher;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, UserService userService, JwtService jwtService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    /** Builds the auth payload with a freshly issued JWT. */
    public AuthResponse issueResponse(User u) {
        return new AuthResponse(u.getUsername(), u.getRating(), u.getProblemsSolved(),
                u.getRole(), jwtService.issue(u));
    }

    @Transactional
    public User register(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalStateException("Username '" + username + "' is already taken");
        }
        User u = new User(username);
        u.setPasswordHash(PasswordHasher.hash(password));
        u.setRole(Role.USER);
        User saved = userRepository.save(u);
        userService.broadcastLeaderboard();
        return saved;
    }

    @Transactional(readOnly = true)
    public User login(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(user -> user.getPasswordHash() != null
                        && PasswordHasher.verify(password, user.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid username or password"));
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
