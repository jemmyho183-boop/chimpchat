package com.chimpchat.api.app.business;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.chimpchat.api.app.dto.AppBootstrapResponseDto;
import com.chimpchat.api.auth.dto.CurrentUserResponseDto;
import com.chimpchat.api.auth.entity.UserEntity;
import com.chimpchat.api.auth.repository.UserRepository;
import com.chimpchat.api.channel.business.InstagramChannelBusiness;
import com.chimpchat.api.channel.business.MetaInstagramBusiness;
import com.chimpchat.api.channel.business.TokenEncryptionBusiness;
import com.chimpchat.api.channel.dto.ChannelStatusResponseDto;
import com.chimpchat.api.workspace.business.WorkspaceBusiness;
import com.chimpchat.api.workspace.dto.WorkspaceResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AppBootstrapBusiness {
    private final UserRepository userRepository;
    private final WorkspaceBusiness workspaceBusiness;
    private final InstagramChannelBusiness instagramChannelBusiness;
    private final MetaInstagramBusiness metaInstagramBusiness;
    private final TokenEncryptionBusiness tokenEncryptionBusiness;

    public AppBootstrapBusiness(
            UserRepository userRepository,
            WorkspaceBusiness workspaceBusiness,
            InstagramChannelBusiness instagramChannelBusiness,
            MetaInstagramBusiness metaInstagramBusiness,
            TokenEncryptionBusiness tokenEncryptionBusiness) {
        this.userRepository = userRepository;
        this.workspaceBusiness = workspaceBusiness;
        this.instagramChannelBusiness = instagramChannelBusiness;
        this.metaInstagramBusiness = metaInstagramBusiness;
        this.tokenEncryptionBusiness = tokenEncryptionBusiness;
    }

    @Transactional(readOnly = true)
    public AppBootstrapResponseDto get(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        var workspace = workspaceBusiness.requirePersonalWorkspace(userId);
        ChannelStatusResponseDto instagram = instagramChannelBusiness.status(userId);
        boolean instagramReady = metaInstagramBusiness.isConfigured()
                && tokenEncryptionBusiness.isConfigured();
        if (!instagramReady && !instagram.status().equals("CONNECTED")) {
            instagram = new ChannelStatusResponseDto(
                    instagram.provider(), "SETUP_REQUIRED", null, instagram.accounts());
        }
        List<ChannelStatusResponseDto> channels = new ArrayList<>();
        channels.add(instagram);
        channels.add(new ChannelStatusResponseDto("WHATSAPP", "COMING_SOON", null, List.of()));
        channels.add(new ChannelStatusResponseDto("FACEBOOK", "COMING_SOON", null, List.of()));
        boolean connected = instagram.status().equals("CONNECTED");
        String nextStep = connected ? "DASHBOARD"
                : instagramReady ? "CONNECT_CHANNEL" : "CHANNEL_SETUP_REQUIRED";
        return new AppBootstrapResponseDto(
                new CurrentUserResponseDto(user.getId(), user.getEmail()),
                new WorkspaceResponseDto(workspace.getId(), workspace.getName()),
                nextStep,
                channels);
    }
}
