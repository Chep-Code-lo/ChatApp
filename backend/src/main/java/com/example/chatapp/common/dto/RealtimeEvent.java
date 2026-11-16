package com.example.chatapp.common.dto;

import java.time.Instant;
import java.util.UUID;

public record RealtimeEvent <T>(String eventId, String type, Instant occurredAt, T data) {
    public static <T> RealtimeEvent <T> of(String type, T data){
        return new RealtimeEvent<T>(UUID.randomUUID().toString(), type, Instant.now(), data);
    }
}
