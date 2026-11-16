package com.example.chatapp.common.security;

import java.util.Objects;
import java.util.UUID;

public record CurrentUser(Long userId, UUID sessionId) {
    public CurrentUser{
        Objects.requireNonNull(userId, "userId không được null");
        Objects.requireNonNull(sessionId, "sessionId không được null");

        if (userId <= 0) {
            throw new IllegalArgumentException("userId phải lớn hơn 0");
        }

    }
}