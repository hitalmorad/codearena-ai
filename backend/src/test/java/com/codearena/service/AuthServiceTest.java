package com.codearena.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codearena.model.Role;
import com.codearena.model.User;
import com.codearena.repository.UserRepository;
import com.codearena.security.JwtService;
import com.codearena.security.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    private JwtService jwtService;

    private AuthService auth;

    @BeforeEach
    void setUp() {
        auth = new AuthService(userRepository, userService, jwtService,
                "admin@codearena.dev, boss@company.com");
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void register_withAllowlistedEmail_grantsAdminRole() {
        when(userRepository.existsByUsername("root")).thenReturn(false);
        when(userRepository.existsByEmail("admin@codearena.dev")).thenReturn(false);

        User u = auth.register("root", "admin@codearena.dev", "secret");

        assertEquals(Role.ADMIN, u.getRole());
    }

    @Test
    void register_isCaseInsensitiveForAdminEmail() {
        when(userRepository.existsByUsername("root")).thenReturn(false);
        when(userRepository.existsByEmail("admin@codearena.dev")).thenReturn(false);

        User u = auth.register("root", "  ADMIN@Codearena.DEV ", "secret");

        assertEquals(Role.ADMIN, u.getRole());
        assertEquals("admin@codearena.dev", u.getEmail(), "email is normalized to lowercase");
    }

    @Test
    void register_withOrdinaryEmail_isPlainUser() {
        when(userRepository.existsByUsername("joe")).thenReturn(false);
        when(userRepository.existsByEmail("joe@example.com")).thenReturn(false);

        User u = auth.register("joe", "joe@example.com", "secret");

        assertEquals(Role.USER, u.getRole());
    }

    @Test
    void register_missingEmail_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> auth.register("joe", "  ", "secret"));
    }

    @Test
    void register_duplicateEmail_isRejected() {
        when(userRepository.existsByUsername("joe")).thenReturn(false);
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> auth.register("joe", "taken@example.com", "secret"));
    }

    @Test
    void login_upgradesToAdminWhenEmailIsAllowlisted() {
        User existing = new User("root");
        existing.setEmail("boss@company.com");
        existing.setRole(Role.USER);
        existing.setPasswordHash(PasswordHasher.hash("secret"));
        when(userRepository.findByUsername("root")).thenReturn(java.util.Optional.of(existing));

        User u = auth.login("root", "secret");

        assertEquals(Role.ADMIN, u.getRole());
        verify(userRepository).save(existing);
    }

    @Test
    void login_ordinaryUserStaysUser() {
        User existing = new User("joe");
        existing.setEmail("joe@example.com");
        existing.setRole(Role.USER);
        existing.setPasswordHash(PasswordHasher.hash("secret"));
        when(userRepository.findByUsername("joe")).thenReturn(java.util.Optional.of(existing));

        User u = auth.login("joe", "secret");

        assertEquals(Role.USER, u.getRole());
        verify(userRepository, never()).save(any());
    }

    @Test
    void isAdminEmail_matchesAllowlistCaseInsensitively() {
        assertTrue(auth.isAdminEmail("BOSS@company.com"));
        assertTrue(auth.isAdminEmail("admin@codearena.dev"));
        org.junit.jupiter.api.Assertions.assertFalse(auth.isAdminEmail("nobody@x.com"));
        org.junit.jupiter.api.Assertions.assertFalse(auth.isAdminEmail(null));
    }
}
