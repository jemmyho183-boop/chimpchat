package com.chimpchat.api.automation.business;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.chimpchat.api.automation.dto.InstagramCommentAutomationRequestDto;
import com.chimpchat.api.automation.dto.InstagramCommentAutomationResponseDto;
import com.chimpchat.api.automation.dto.InstagramMediaResponseDto;
import com.chimpchat.api.automation.entity.AutomationStepExecutionEntity;
import com.chimpchat.api.automation.entity.InstagramCommentAutomationEntity;
import com.chimpchat.api.automation.entity.InstagramCommentAutomationExecutionEntity;
import com.chimpchat.api.automation.repository.AutomationStepExecutionRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class InstagramCommentAutomationBusiness {
    private static final Logger logger = LoggerFactory.getLogger(InstagramCommentAutomationBusiness.class);
    private static final String PROVIDER = "INSTAGRAM";

    private final AutomationDefinitionMapper definitionMapper;
    private final AutomationTriggerHandlerRegistry triggerHandlerRegistry;
    private final AutomationStepHandlerRegistry stepHandlerRegistry;
    private final InstagramCommentAutomationRepository automationRepository;
    private final InstagramCommentAutomationExecutionRepository executionRepository;
    private final AutomationStepExecutionRepository stepExecutionRepository;
    private final InstagramCommentRepository commentRepository;
    private final ChannelConnectionRepository connectionRepository;
    private final WorkspaceBusiness workspaceBusiness;
    private final TokenEncryptionBusiness tokenEncryptionBusiness;
    private final InstagramCommentAutomationMetaBusiness metaBusiness;
    private final TransactionTemplate transactionTemplate;

    public InstagramCommentAutomationBusiness(
            AutomationDefinitionMapper definitionMapper,
            AutomationTriggerHandlerRegistry triggerHandlerRegistry,
            AutomationStepHandlerRegistry stepHandlerRegistry,
            InstagramCommentAutomationRepository automationRepository,
            InstagramCommentAutomationExecutionRepository executionRepository,
            AutomationStepExecutionRepository stepExecutionRepository,
            InstagramCommentRepository commentRepository,
            ChannelConnectionRepository connectionRepository,
            WorkspaceBusiness workspaceBusiness,
            TokenEncryptionBusiness tokenEncryptionBusiness,
            InstagramCommentAutomationMetaBusiness metaBusiness,
            TransactionTemplate transactionTemplate) {
        this.definitionMapper = definitionMapper;
        this.triggerHandlerRegistry = triggerHandlerRegistry;
        this.stepHandlerRegistry = stepHandlerRegistry;
        this.automationRepository = automationRepository;
        this.executionRepository = executionRepository;
        this.stepExecutionRepository = stepExecutionRepository;
        this.commentRepository = commentRepository;
        this.connectionRepository = connectionRepository;
        this.workspaceBusiness = workspaceBusiness;
        this.tokenEncryptionBusiness = tokenEncryptionBusiness;
        this.metaBusiness = metaBusiness;
        this.transactionTemplate = transactionTemplate;
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
        String definitionJson = definitionMapper.toJson(definitionMapper.fromRequest(request));
        InstagramCommentAutomationEntity automation = new InstagramCommentAutomationEntity(
                connection,
                request.accountId(),
                request.mediaId(),
                request.mediaLabel().trim(),
                request.matchType(),
                normalizedKeyword(request),
                request.publicReply().trim(),
                request.directMessage().trim(),
                normalizedLinkText(request),
                normalizedLinkUrl(request));
        automation.initializeDefinition(definitionJson);
        automationRepository.save(automation);
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
        automation.updateDefinition(definitionMapper.toJson(definitionMapper.fromRequest(request)));
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
        AutomationEvent event = commentEvent(comment);
        List<InstagramCommentAutomationEntity> automations = automationRepository.findEnabledForMedia(
                comment.getConnection().getId(),
                comment.getInstagramAccountId(),
                comment.getExternalMediaId());
        for (InstagramCommentAutomationEntity automation : automations) {
            AutomationDefinition definition = currentDefinition(automation);
            AutomationTriggerHandler triggerHandler =
                    triggerHandlerRegistry.require(definition.trigger().type());
            triggerHandler.validate(definition.trigger());
            if (!triggerHandler.matches(definition.trigger(), event)
                    || executionRepository.existsByCommentIdAndAutomationId(comment.getId(), automation.getId())) {
                continue;
            }

            definition.steps().forEach(stepHandlerRegistry::validate);
            List<AutomationStepHandler> handlers = definition.steps().stream()
                    .map(step -> stepHandlerRegistry.require(step.type()))
                    .toList();
            InstagramCommentAutomationExecutionEntity execution = executionRepository.saveAndFlush(
                    new InstagramCommentAutomationExecutionEntity(
                            comment, automation, definitionMapper.toJson(definition)));
            String accessToken;
            try {
                accessToken = tokenEncryptionBusiness.decrypt(
                        automation.getConnection().getEncryptedAccessToken());
            } catch (TokenEncryptionBusiness.ChannelException exception) {
                execution.markPublicReplyFailed(false);
                execution.markDirectMessageFailed(false);
                logger.warn("Instagram automation {} could not load its account credentials.", automation.getId());
                persistFailedSteps(execution, definition, exception.getMessage(), false);
                continue;
            }

            for (int index = 0; index < definition.steps().size(); index++) {
                AutomationDefinition.StepDefinition step = definition.steps().get(index);
                AutomationStepHandler handler = handlers.get(index);
                AutomationStepExecutionEntity stepExecution = stepExecutionRepository.saveAndFlush(
                        new AutomationStepExecutionEntity(execution, step.id(), step.type()));
                try {
                    String providerResultId = handler.execute(step, automation, comment, accessToken);
                    stepExecution.markSent(providerResultId);
                    markLegacyStepSent(execution, step, providerResultId);
                } catch (InstagramCommentAutomationMetaBusiness.MetaRequestException exception) {
                    stepExecution.markFailed(exception.isOutcomeUnknown(), exception.getMessage());
                    markLegacyStepFailed(execution, step, exception.isOutcomeUnknown());
                    logger.warn("Instagram automation {} step {} failed: {}",
                            automation.getId(), step.id(), exception.getMessage());
                }
                stepExecutionRepository.save(stepExecution);
            }
        }
    }

    public void processDirectMessageReply(
            String instagramAccountId,
            String commenterId,
            String replyToMessageId,
            Instant receivedAt) {
        if (instagramAccountId == null || instagramAccountId.isBlank()
                || commenterId == null || commenterId.isBlank()
                || receivedAt == null) {
            return;
        }
        if (receivedAt.isBefore(Instant.now().minus(Duration.ofHours(24)))) {
            return;
        }

        FollowUpCandidate candidate = transactionTemplate.execute(status -> {
            List<InstagramCommentAutomationExecutionEntity> executions =
                    executionRepository.findPendingLinkButtonReply(
                            instagramAccountId,
                            commenterId,
                            replyToMessageId,
                            PageRequest.of(0, 1));
            if (executions.isEmpty()) {
                return null;
            }
            InstagramCommentAutomationExecutionEntity execution = executions.get(0);
            execution.markLinkButtonProcessing();
            executionRepository.saveAndFlush(execution);
            InstagramCommentAutomationEntity automation = execution.getAutomation();
            AutomationDefinition definition = execution.getDefinitionSnapshot() == null
                    ? definitionMapper.fromLegacy(automation)
                    : definitionMapper.fromJson(execution.getDefinitionSnapshot());
            AutomationDefinition.StepDefinition privateReply = definition.steps().stream()
                    .filter(step -> AutomationDefinition.INSTAGRAM_PRIVATE_REPLY.equals(step.type()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "The automation execution has no private-reply step."));
            return new FollowUpCandidate(
                    execution.getId(),
                    automation.getInstagramAccountId(),
                    execution.getComment().getCommenterId(),
                    automation.getConnection().getEncryptedAccessToken(),
                    privateReply.config().get("buttonText"),
                    privateReply.config().get("buttonUrl"));
        });
        if (candidate == null) {
            return;
        }

        try {
            String accessToken = tokenEncryptionBusiness.decrypt(candidate.encryptedAccessToken());
            metaBusiness.sendLinkButtonFollowUp(
                    candidate.instagramAccountId(),
                    candidate.commenterId(),
                    accessToken,
                    candidate.buttonText(),
                    candidate.linkUrl());
            updateLinkButtonStatus(candidate.executionId(), true, false);
        } catch (TokenEncryptionBusiness.ChannelException exception) {
            updateLinkButtonStatus(candidate.executionId(), false, false);
            logger.warn("Instagram link button could not load its account credentials.");
        } catch (InstagramCommentAutomationMetaBusiness.MetaRequestException exception) {
            updateLinkButtonStatus(candidate.executionId(), false, exception.isOutcomeUnknown());
            logger.warn("Instagram link button failed for automation execution {}: {}",
                    candidate.executionId(), exception.getMessage());
        }
    }

    private void updateLinkButtonStatus(UUID executionId, boolean sent, boolean outcomeUnknown) {
        transactionTemplate.executeWithoutResult(status -> {
            InstagramCommentAutomationExecutionEntity execution = executionRepository.findById(executionId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Instagram automation execution disappeared before its link button was recorded."));
            if (sent) {
                execution.markLinkButtonSent();
            } else {
                execution.markLinkButtonFailed(outcomeUnknown);
            }
            executionRepository.save(execution);
        });
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
        if (request.directMessage().trim().getBytes(StandardCharsets.UTF_8).length > 1000) {
            throw invalidDmMessage("The private DM must fit within Instagram's 1,000-byte message limit.");
        }
        boolean hasLinkText = request.directMessageLinkText() != null
                && !request.directMessageLinkText().isBlank();
        boolean hasLinkUrl = request.directMessageLinkUrl() != null
                && !request.directMessageLinkUrl().isBlank();
        if (hasLinkText != hasLinkUrl) {
            throw invalidDmLink("Provide both the DM link text and URL, or leave both empty.");
        }
        if (hasLinkUrl) {
            if (request.directMessageLinkText().trim().length() > 20) {
                throw invalidDmLink("The link button label must be 20 characters or fewer.");
            }
            validateDmLinkUrl(request.directMessageLinkUrl().trim());
        }
    }

    private InstagramCommentAutomationException invalidDmMessage(String message) {
        return new InstagramCommentAutomationException(
                HttpStatus.BAD_REQUEST, "invalid_dm_message", message);
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

    private AutomationDefinition currentDefinition(InstagramCommentAutomationEntity automation) {
        return automation.getDefinitionJson() == null
                ? definitionMapper.fromLegacy(automation)
                : definitionMapper.fromJson(automation.getDefinitionJson());
    }

    private AutomationEvent commentEvent(InstagramCommentEntity comment) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("mediaId", comment.getExternalMediaId());
        if (comment.getCommentText() != null) {
            attributes.put("commentText", comment.getCommentText());
        }
        return new AutomationEvent(AutomationDefinition.INSTAGRAM_COMMENT_RECEIVED, attributes);
    }

    private void persistFailedSteps(
            InstagramCommentAutomationExecutionEntity execution,
            AutomationDefinition definition,
            String message,
            boolean outcomeUnknown) {
        for (AutomationDefinition.StepDefinition step : definition.steps()) {
            AutomationStepExecutionEntity stepExecution = new AutomationStepExecutionEntity(
                    execution, step.id(), step.type());
            stepExecution.markFailed(outcomeUnknown, message);
            stepExecutionRepository.save(stepExecution);
        }
    }

    private void markLegacyStepSent(
            InstagramCommentAutomationExecutionEntity execution,
            AutomationDefinition.StepDefinition step,
            String providerResultId) {
        if (AutomationDefinition.INSTAGRAM_PUBLIC_COMMENT_REPLY.equals(step.type())) {
            execution.markPublicReplySent();
        } else if (AutomationDefinition.INSTAGRAM_PRIVATE_REPLY.equals(step.type())) {
            execution.markDirectMessageSent(
                    providerResultId,
                    step.config().containsKey("buttonUrl"));
        }
    }

    private void markLegacyStepFailed(
            InstagramCommentAutomationExecutionEntity execution,
            AutomationDefinition.StepDefinition step,
            boolean outcomeUnknown) {
        if (AutomationDefinition.INSTAGRAM_PUBLIC_COMMENT_REPLY.equals(step.type())) {
            execution.markPublicReplyFailed(outcomeUnknown);
        } else if (AutomationDefinition.INSTAGRAM_PRIVATE_REPLY.equals(step.type())) {
            execution.markDirectMessageFailed(outcomeUnknown);
        }
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

    private record FollowUpCandidate(
            UUID executionId,
            String instagramAccountId,
            String commenterId,
            String encryptedAccessToken,
            String buttonText,
            String linkUrl) {}
}
