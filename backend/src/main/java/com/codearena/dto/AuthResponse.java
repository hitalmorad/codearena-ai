package com.codearena.dto;

import com.codearena.model.Role;
import com.codearena.model.User;

public record AuthResponse(
        String username,
        int rating,
        int problemsSolved,
        Role role,
        String token
) {
    public static AuthResponse from(User u) {
        return new AuthResponse(u.getUsername(), u.getRating(), u.getProblemsSolved(), u.getRole(), u.getToken());
    }
}
