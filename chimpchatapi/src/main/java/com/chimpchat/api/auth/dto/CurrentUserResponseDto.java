package com.chimpchat.api.auth.dto;

import java.util.UUID;

public record CurrentUserResponseDto(UUID id, String email) {}
