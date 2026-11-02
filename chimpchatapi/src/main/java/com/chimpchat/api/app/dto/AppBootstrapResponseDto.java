package com.chimpchat.api.app.dto;

import java.util.List;

import com.chimpchat.api.auth.dto.CurrentUserResponseDto;
import com.chimpchat.api.channel.dto.ChannelStatusResponseDto;
import com.chimpchat.api.workspace.dto.WorkspaceResponseDto;

public record AppBootstrapResponseDto(
        CurrentUserResponseDto user,
        WorkspaceResponseDto workspace,
        String nextStep,
        List<ChannelStatusResponseDto> channels) {}
