package com.example.chatapp.common.dto;

public record AttachmentDto(String fileId, Kind kind, String originalName, String mimeType, long sizeBytes, Long durationMS, Integer width, Integer height, boolean thumbnaiAvailable) {
    public enum Kind {
        IMAGE, VIDEO, FILE, VOICE
    }
}