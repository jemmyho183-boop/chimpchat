package com.chimpchat.api.automation.business;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.chimpchat.api.automation.dto.InstagramCommentAutomationRequestDto;
import com.chimpchat.api.automation.dto.InstagramCommentAutomationResponseDto;
import com.chimpchat.api.automation.dto.InstagramMediaResponseDto;
import com.chimpchat.api.automation.entity.InstagramCommentAutomationEntity;
import com.chimpchat.api.automation.entity.InstagramCommentAutomationExecutionEntity;
import com.chimpchat.api.automation.repository.InstagramCommentAutomationExecutionRepository;
import com.chimpchat.api.automation.repository.InstagramCommentAutomationRepository;
import com.chimpchat.api.channel.business.TokenEncryptionBusiness;
import com.chimpchat.api.channel.entity.ChannelAccountEntity;
import com.chimpchat.api.channel.entity.ChannelConnectionEntity;
import com.chimpchat.api.channel.entity.InstagramCommentEntity;
import com.chimpchat.api.channel.repository.ChannelConnectionRepository;
import com.chimpchat.api.channel.repository.InstagramCommentRepository;
import com.chimpchat.api.workspace.business.WorkspaceBusiness;
import com.chimpchat.api.workspace.entity.WorkspaceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstagramCommentAutomationBusiness {
    private static final Logger logger = LoggerFactory.getLogger(InstagramCommentAutomationBusiness.class);
    private static final String PROVIDER = "INSTAGRAM";

    private final InstagramCommentAutomationRepository automationRepository;
    private final InstagramCommentAutomationExecutionRepository executionRepository;
    private final InstagramCommentRepository commentRepository;
    private final ChannelConnectionRepository connectionRepository;
    private final WorkspaceBusiness workspaceBusiness;
    private final TokenEncryptionBusiness tokenEncryptionBusiness;
    private final InstagramCommentAutomationMetaBusiness metaBusiness;

    public InstagramCommentAutomationBusiness(
            InstagramCommentAutomationRepository automationRepository,
            InstagramCommentAutomationExecutionRepository executionRepository,
            InstagramCommentRepository commentRepository,
            ChannelConnectionRepository connectionRepository,
            WorkspaceBusiness workspaceBusiness,
            TokenEncryptionBusiness tokenEncryptionBusiness,
            InstagramCommentAutomationMetaBusiness metaBusiness) {
        this.automationRepository = automationRepository;
        this.executionRepository = executionRepository;
        this.commentRepository = commentRepository;
        this.connectionRepository = connectionRepository;
        this.workspaceBusiness = workspaceBusiness;
        this.tokenEncryptionBusiness = tokenEncryptionBusiness;
        this.metaBusiness = metaBusiness;
    }

    @Transactional(readOnly = true)
    public List<InstagramMediaResponseDto> listPosts(UUID ownerId, String accountId) {
        WorkspaceEntity workspace = workspaceBusiness.requirePersonalWorkspace(ownerId);
        ChannelConnectionEntity connection = requireConnection(workspace, accountId);
        String accessToken = tokenEncryptionBusiness.decrypt(connection.getEncryptedAccessToken());
        try {
            return metaBusiness.listMedia(accountId, accessToken).stream()
                    .map(media -> new InstagramMediaResponseDto(
                            media.id(),
                            media.caption(),
                            media.mediaType(),
                            media.thumbnailUrl(),
                            media.mediaUrl(),
                            media.permalink(),
                            media.timestamp()))
                    .toList();
        } catch (InstagramCommentAutomationMetaBusiness.MetaRequestException exception) {
            throw new InstagramCommentAutomationException(
                    HttpStatus.BAD_GATEWAY, "instagram_media_unavailable",
                    "Instagram posts could not be loaded: " + exception.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<InstagramCommentAutomationResponseDto> list(UUID ownerId, String accountId) {
        WorkspaceEntity workspace = workspaceBusiness.requirePersonalWorkspace(ownerId);
        if (accountId != null && accountId.isBlank()) {
            throw new InstagramCommentAutomationException(
                    HttpStatus.BAD_REQUEST, "invalid_account_id", "The Instagram account filter is invalid.");
        }
        return automationRepository.findAllForWorkspace(workspace.getId(), accountId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InstagramCommentAutomationResponseDto create(
            UUID ownerId, InstagramCommentAutomationRequestDto request) {
        WorkspaceEntity workspace = workspaceBusiness.requirePersonalWorkspace(ownerId);
        ChannelConnectionEntity connection = requireConnection(workspace, request.accountId());
        validateRequest(request);
        if (automationRepository.existsByConnectionIdAndInstagramAccountIdAndExternalMediaId(
                connection.getId(), request.accountId(), request.mediaId())) {
            throw new InstagramCommentAutomationException(
                    HttpStatus.CONFLICT, "automation_already_exists",
                    "A flow already exists for this Instagram post and account.");
        }
        InstagramCommentAutomationEntity automation = automationRepository.save(
                new InstagramCommentAutomationEntity(
                        connection,
                        request.accountId(),
                        request.mediaId(),
                        request.mediaLabel().trim(),
                        request.matchType(),
                        normalizedKeyword(request),
                        request.publicReply().trim(),
                        request.directMessage().trim(),
                        normalizedLinkText(request),
                        normalizedLinkUrl(request)));
        return toResponse(automation);
    }

    @Transactional
    public InstagramCommentAutomationResponseDto update(
            UUID ownerId, UUID automationId, InstagramCommentAutomationRequestDto request) {
        WorkspaceEntity workspace = workspaceBusiness.requirePersonalWorkspace(ownerId);
        InstagramCommentAutomationEntity automation = automationRepository
                .findByIdForWorkspace(automationId, workspace.getId())
                .orElseThrow(() -> notFound());
        if (!automation.getInstagramAccountId().equals(request.accountId())) {
            throw new InstagramCommentAutomationException(
                    HttpStatus.BAD_REQUEST, "automation_account_immutable",
                    "An automation cannot be moved to another Instagram account.");
        }
        if (!automation.getExternalMediaId().equals(request.mediaId())) {
            throw new InstagramCommentAutomationException(
                    HttpStatus.BAD_REQUEST, "automation_post_immutable",
                    "An automation cannot be moved to another Instagram post.");
        }
        validateRequest(request);
        automation.update(
                request.mediaLabel().trim(),
                request.matchType(),
                normalizedKeyword(request),
                request.publicReply().trim(),
                request.directMessage().trim(),
                normalizedLinkText(request),
                normalizedLinkUrl(request),
                request.enabled());
        return toResponse(automationRepository.save(automation));
    }

    @Transactional
    public void delete(UUID ownerId, UUID automationId) {
        WorkspaceEntity workspace = workspaceBusiness.requirePersonalWorkspace(ownerId);
        InstagramCommentAutomationEntity automation = automationRepository
                .findByIdForWorkspace(automationId, workspace.getId())
                .orElseThrow(() -> notFound());
        automationRepository.delete(automation);
    }

    @Transactional
    public void processComment(UUID commentId) {
        InstagramCommentEntity comment = commentRepository.findWithConnectionById(commentId)
                .orElseThrow(() -> new IllegalStateException(
                        "Instagram comment disappeared before its automation was processed."));
        if (comment.isDeleted() || comment.getExternalMediaId() == null) {
            return;
        }
        List<InstagramCommentAutomationEntity> automations = automationRepository.findEnabledForMedia(
                comment.getConnection().getId(),
                comment.getInstagramAccountId(),
                comment.getExternalMediaId());
        for (InstagramCommentAutomationEntity automation : automations) {
            if (!matches(automation, comment.getCommentText())
                    || executionRepository.existsByCommentIdAndAutomationId(comment.getId(), automation.getId())) {
                continue;
            }

            InstagramCommentAutomationExecutionEntity execution = executionRepository.saveAndFlush(
                    new InstagramCommentAutomationExecutionEntity(comment, automation));
            String accessToken;
            try {
                accessToken = tokenEncryptionBusiness.decrypt(
                        automation.getConnection().getEncryptedAccessToken());
            } catch (TokenEncryptionBusiness.ChannelException exception) {
                execution.markPublicReplyFailed(false);
                execution.markDirectMessageFailed(false);
                logger.warn("Instagram automation {} could not load its account credentials.", automation.getId());
                continue;
            }

            try {
                metaBusiness.replyToComment(
                        comment.getExternalCommentId(), accessToken, automation.getPublicReply());
                execution.markPublicReplySent();
            } catch (InstagramCommentAutomationMetaBusiness.MetaRequestException exception) {
                execution.markPublicReplyFailed(exception.isOutcomeUnknown());
                logger.warn("Instagram public reply failed for automation {}: {}",
                        automation.getId(), exception.getMessage());
            }

            try {
                metaBusiness.sendPrivateReply(
                        comment.getInstagramAccountId(),
                        comment.getExternalCommentId(),
                        accessToken,
                        automation.getDirectMessage(),
                        automation.getDirectMessageLinkText(),
                        automation.getDirectMessageLinkUrl());
                execution.markDirectMessageSent();
            } catch (InstagramCommentAutomationMetaBusiness.MetaRequestException exception) {
                execution.markDirectMessageFailed(exception.isOutcomeUnknown());
                logger.warn("Instagram DM failed for automation {}: {}",
                        automation.getId(), exception.getMessage());
            }
        }
    }

    private ChannelConnectionEntity requireConnection(WorkspaceEntity workspace, String accountId) {
        if (accountId == null || accountId.isBlank() || accountId.length() > 255) {
            throw new InstagramCommentAutomationException(
                    HttpStatus.BAD_REQUEST, "invalid_account_id", "The Instagram account is invalid.");
        }
        ChannelConnectionEntity connection = connectionRepository
                .findByWorkspaceIdAndProvider(workspace.getId(), PROVIDER)
                .orElseThrow(() -> new InstagramCommentAutomationException(
                        HttpStatus.NOT_FOUND, "instagram_account_not_found",
                        "The connected Instagram account was not found."));
        boolean ownsAccount = connection.getAccounts().stream()
                .map(ChannelAccountEntity::getExternalAccountId)
                .anyMatch(accountId::equals);
        if (!ownsAccount) {
            throw new InstagramCommentAutomationException(
                    HttpStatus.NOT_FOUND, "instagram_account_not_found",
                    "The connected Instagram account was not found.");
        }
        return connection;
    }

    private void validateRequest(InstagramCommentAutomationRequestDto request) {
        if (InstagramCommentAutomationEntity.KEYWORD.equals(request.matchType())
                && (request.keyword() == null || request.keyword().isBlank())) {
            throw new InstagramCommentAutomationException(
                    HttpStatus.BAD_REQUEST, "keyword_required",
                    "Add a keyword or choose the any-comment trigger.");
        }
        boolean hasLinkText = request.directMessageLinkText() != null
                && !request.directMessageLinkText().isBlank();
        boolean hasLinkUrl = request.directMessageLinkUrl() != null
                && !request.directMessageLinkUrl().isBlank();
        if (hasLinkText != hasLinkUrl) {
            throw invalidDmLink("Provide both the DM link text and URL, or leave both empty.");
        }
        if (hasLinkUrl) {
            validateDmLinkUrl(request.directMessageLinkUrl().trim());
            String completeMessage = request.directMessage().trim() + "\n\n"
                    + request.directMessageLinkText().trim() + ": "
                    + request.directMessageLinkUrl().trim();
            if (completeMessage.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 1000) {
                throw invalidDmLink("The DM text and link must fit within Instagram's 1,000-byte message limit.");
            }
        }
    }

    private void validateDmLinkUrl(String value) {
        java.net.URI uri;
        try {
            uri = java.net.URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw invalidDmLink("Use a valid HTTP or HTTPS URL for the DM link.");
        }
        String scheme = uri.getScheme();
        if (uri.getHost() == null || uri.getRawUserInfo() != null
                || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            throw invalidDmLink("Use a valid HTTP or HTTPS URL for the DM link.");
        }
    }

    private InstagramCommentAutomationException invalidDmLink(String message) {
        return new InstagramCommentAutomationException(
                HttpStatus.BAD_REQUEST, "invalid_dm_link", message);
    }

    private String normalizedLinkText(InstagramCommentAutomationRequestDto request) {
        return request.directMessageLinkText() == null || request.directMessageLinkText().isBlank()
                ? null
                : request.directMessageLinkText().trim();
    }

    private String normalizedLinkUrl(InstagramCommentAutomationRequestDto request) {
        return request.directMessageLinkUrl() == null || request.directMessageLinkUrl().isBlank()
                ? null
                : request.directMessageLinkUrl().trim();
    }

    private String normalizedKeyword(InstagramCommentAutomationRequestDto request) {
        return InstagramCommentAutomationEntity.KEYWORD.equals(request.matchType())
                ? request.keyword().trim()
                : null;
    }

    private boolean matches(InstagramCommentAutomationEntity automation, String commentText) {
        if (InstagramCommentAutomationEntity.ANY_COMMENT.equals(automation.getMatchType())) {
            return true;
        }
        return commentText != null
                && commentText.toLowerCase(Locale.ROOT)
                        .contains(automation.getKeyword().toLowerCase(Locale.ROOT));
    }

    private InstagramCommentAutomationResponseDto toResponse(InstagramCommentAutomationEntity automation) {
        return new InstagramCommentAutomationResponseDto(
                automation.getId(),
                automation.getInstagramAccountId(),
                automation.getExternalMediaId(),
                automation.getMediaLabel(),
                automation.getMatchType(),
                automation.getKeyword(),
                automation.getPublicReply(),
                automation.getDirectMessage(),
                automation.getDirectMessageLinkText(),
                automation.getDirectMessageLinkUrl(),
                automation.isEnabled(),
                automation.getCreatedAt());
    }

    private InstagramCommentAutomationException notFound() {
        return new InstagramCommentAutomationException(
                HttpStatus.NOT_FOUND, "automation_not_found", "The Instagram automation was not found.");
    }
}
