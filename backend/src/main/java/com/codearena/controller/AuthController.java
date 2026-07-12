package com.codearena.controller;

import com.codearena.dto.AuthRequest;
import com.codearena.dto.AuthResponse;
import com.codearena.dto.ChangePasswordRequest;
import com.codearena.dto.UpdateBioRequest;
import com.codearena.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody AuthRequest request) {
        return authService.issueResponse(authService.register(request.username(), request.password()));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        return authService.issueResponse(authService.login(request.username(), request.password()));
    }

    @GetMapping("/me")
    public AuthResponse me(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        // Re-issues a fresh (sliding) token so active users stay signed in.
        return authService.resolveToken(token)
                .map(authService::issueResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }

    @PutMapping("/me/bio")
    public AuthResponse updateBio(@RequestHeader(value = "X-Auth-Token", required = false) String token,
                                 @Valid @RequestBody UpdateBioRequest request) {
        return authService.issueResponse(authService.updateBio(token, request.bio()));
    }

    @PutMapping("/me/password")
    public AuthResponse changePassword(@RequestHeader(value = "X-Auth-Token", required = false) String token,
                                       @Valid @RequestBody ChangePasswordRequest request) {
        return authService.issueResponse(
                authService.changePassword(token, request.currentPassword(), request.newPassword()));
    }
}
