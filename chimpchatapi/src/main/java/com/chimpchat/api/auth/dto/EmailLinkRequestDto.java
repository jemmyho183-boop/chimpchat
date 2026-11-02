package com.chimpchat.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailLinkRequestDto(
        @NotBlank @Email @Size(max = 320) String email) {}
