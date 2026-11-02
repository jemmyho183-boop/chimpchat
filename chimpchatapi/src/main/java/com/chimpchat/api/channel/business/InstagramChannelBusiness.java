package com.chimpchat.api.channel.business;

import java.util.List;
import java.util.UUID;

import com.chimpchat.api.channel.business.MetaInstagramBusiness.InstagramAccount;
import com.chimpchat.api.channel.business.MetaInstagramBusiness.MetaConnection;
import com.chimpchat.api.channel.dto.ChannelAccountResponseDto;
import com.chimpchat.api.channel.dto.ChannelStatusResponseDto;
import com.chimpchat.api.channel.entity.ChannelAccountEntity;
import com.chimpchat.api.channel.entity.ChannelConnectionEntity;
import com.chimpchat.api.channel.repository.ChannelConnectionRepository;
import com.chimpchat.api.workspace.business.WorkspaceBusiness;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstagramChannelBusiness {
    private static final String PROVIDER = "INSTAGRAM";

    private final ChannelConnectionRepository connectionRepository;
    private final WorkspaceBusiness workspaceBusiness;
    private final TokenEncryptionBusiness tokenEncryptionBusiness;

    public InstagramChannelBusiness(
            ChannelConnectionRepository connectionRepository,
            WorkspaceBusiness workspaceBusiness,
            TokenEncryptionBusiness tokenEncryptionBusiness) {
        this.connectionRepository = connectionRepository;
        this.workspaceBusiness = workspaceBusiness;
        this.tokenEncryptionBusiness = tokenEncryptionBusiness;
    }

    @Transactional
    public ChannelStatusResponseDto connect(UUID ownerId, MetaConnection metaConnection) {
        var workspace = workspaceBusiness.requirePersonalWorkspace(ownerId);
        String encryptedToken = tokenEncryptionBusiness.encrypt(metaConnection.accessToken());
        ChannelConnectionEntity connection = connectionRepository
                .findByWorkspaceIdAndProvider(workspace.getId(), PROVIDER)
                .orElseGet(() -> new ChannelConnectionEntity(
                        workspace, PROVIDER, metaConnection.clientBusinessId(), encryptedToken));

        connection.updateCredentials(metaConnection.clientBusinessId(), encryptedToken);
        connection.replaceAccounts(metaConnection.accounts().stream()
                .map(account -> new ChannelAccountEntity(account.id(), account.username()))
                .toList());
        connectionRepository.save(connection);
        return toResponse(connection);
    }

    @Transactional(readOnly = true)
    public ChannelStatusResponseDto status(UUID ownerId) {
        var workspace = workspaceBusiness.requirePersonalWorkspace(ownerId);
        return connectionRepository.findByWorkspaceIdAndProvider(workspace.getId(), PROVIDER)
                .map(this::toResponse)
                .orElseGet(() -> new ChannelStatusResponseDto(PROVIDER, "NOT_CONNECTED", null, List.of()));
    }

    @Transactional
    public void disconnect(UUID ownerId, UUID connectionId) {
        var workspace = workspaceBusiness.requirePersonalWorkspace(ownerId);
        long deleted = connectionRepository.deleteByIdAndWorkspaceIdAndProvider(
                connectionId, workspace.getId(), PROVIDER);
        if (deleted == 0) {
            throw new ChannelException(HttpStatus.NOT_FOUND, "connection_not_found",
                    "The channel connection was not found.");
        }
    }

    private ChannelStatusResponseDto toResponse(ChannelConnectionEntity connection) {
        List<ChannelAccountResponseDto> accounts = connection.getAccounts().stream()
                .map(account -> new ChannelAccountResponseDto(
                        account.getExternalAccountId(), account.getUsername()))
                .toList();
        return new ChannelStatusResponseDto(PROVIDER, "CONNECTED", connection.getId(), accounts);
    }

    public static class ChannelException extends RuntimeException {
        private final HttpStatus status;
        private final String code;

        public ChannelException(HttpStatus status, String code, String message) {
            super(message);
            this.status = status;
            this.code = code;
        }

        public HttpStatus getStatus() {
            return status;
        }

        public String getCode() {
            return code;
        }
    }
}
