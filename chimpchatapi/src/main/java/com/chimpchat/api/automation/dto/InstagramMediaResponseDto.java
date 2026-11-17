package com.chimpchat.api.automation.dto;

import java.time.Instant;

public record InstagramMediaResponseDto(
        String id,
        String caption,
        String mediaType,
        String thumbnailUrl,
        String mediaUrl,
        String permalink,
        Instant timestamp) {}
