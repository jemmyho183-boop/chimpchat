package com.chimpchat.api.channel.dto;

import java.util.List;
import java.util.UUID;

public record ChannelStatusResponseDto(
        String provider,
        String status,
        UUID connectionId,
        List<ChannelAccountResponseDto> accounts) {}
