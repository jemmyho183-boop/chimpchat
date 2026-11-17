package com.chimpchat.api.automation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InstagramCommentAutomationRequestDto(
        @NotBlank @Size(max = 255) String accountId,
        @NotBlank @Size(max = 255) String mediaId,
        @NotBlank @Size(max = 500) String mediaLabel,
        @NotBlank @Pattern(regexp = "ANY_COMMENT|KEYWORD") String matchType,
        @Size(max = 100) String keyword,
        @NotBlank @Size(max = 2200) String publicReply,
        @NotBlank @Size(max = 1000) String directMessage,
        boolean enabled) {}
