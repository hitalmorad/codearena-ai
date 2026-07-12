package com.codearena.dto;

import com.codearena.model.User;

public record UserDto(
        String username,
        int rating,
        int problemsSolved
) {
    public static UserDto from(User u) {
        return new UserDto(u.getUsername(), u.getRating(), u.getProblemsSolved());
    }
}
