package com.chimpchat.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ConsumeEmailLinkRequestDto(@NotBlank String token) {}
