package com.abhout.pocket_ledger_be.user;

import java.util.UUID;

public record UserResponse(UUID id, String email, boolean emailVerified) {
    public static UserResponse from(User user) {
        return  new UserResponse(
                user.getId(),
                user.getEmail(),
                user.isEmailVerified()
        );
    }
}
