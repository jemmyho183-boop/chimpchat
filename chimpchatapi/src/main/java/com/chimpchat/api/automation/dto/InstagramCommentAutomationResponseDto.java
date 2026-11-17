package com.chimpchat.api.automation.dto;

import java.time.Instant;
import java.util.UUID;

public record InstagramCommentAutomationResponseDto(
        UUID id,
        String accountId,
        String mediaId,
        String mediaLabel,
        String matchType,
        String keyword,
        String publicReply,
        String directMessage,
        boolean enabled,
        Instant createdAt) {}
