package com.chimpchat.api.automation.business;

import java.time.Instant;

public record InstagramDirectMessageReceivedEvent(
        String instagramAccountId,
        String commenterId,
        String replyToMessageId,
        Instant receivedAt) {}
