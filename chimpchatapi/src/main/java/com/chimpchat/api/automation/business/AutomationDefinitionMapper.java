package com.chimpchat.api.automation.business;

import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import com.chimpchat.api.automation.dto.InstagramCommentAutomationRequestDto;
import com.chimpchat.api.automation.entity.InstagramCommentAutomationEntity;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class AutomationDefinitionMapper {
    private final ObjectMapper objectMapper;

    public AutomationDefinitionMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AutomationDefinition fromRequest(InstagramCommentAutomationRequestDto request) {
        Map<String, String> triggerConfig = new TreeMap<>();
        triggerConfig.put("mediaId", request.mediaId());
        triggerConfig.put("mediaLabel", request.mediaLabel().trim());
        triggerConfig.put("matchType", request.matchType());
        putIfPresent(triggerConfig, "keyword", normalizedKeyword(request));

        Map<String, String> publicReplyConfig = new TreeMap<>();
        publicReplyConfig.put("message", request.publicReply().trim());

        Map<String, String> privateReplyConfig = new TreeMap<>();
        privateReplyConfig.put("message", request.directMessage().trim());
        putIfPresent(privateReplyConfig, "buttonText", normalized(request.directMessageLinkText()));
        putIfPresent(privateReplyConfig, "buttonUrl", normalized(request.directMessageLinkUrl()));

        return new AutomationDefinition(
                AutomationDefinition.CURRENT_SCHEMA_VERSION,
                new AutomationDefinition.TriggerDefinition(
                        AutomationDefinition.INSTAGRAM_COMMENT_RECEIVED,
                        AutomationDefinition.CURRENT_SCHEMA_VERSION,
                        triggerConfig),
                List.of(
                        new AutomationDefinition.StepDefinition(
                                "public-reply",
                                AutomationDefinition.INSTAGRAM_PUBLIC_COMMENT_REPLY,
                                AutomationDefinition.CURRENT_SCHEMA_VERSION,
                                publicReplyConfig),
                        new AutomationDefinition.StepDefinition(
                                "opening-dm",
                                AutomationDefinition.INSTAGRAM_PRIVATE_REPLY,
                                AutomationDefinition.CURRENT_SCHEMA_VERSION,
                                privateReplyConfig)));
    }

    public AutomationDefinition fromLegacy(InstagramCommentAutomationEntity automation) {
        Map<String, String> triggerConfig = new TreeMap<>();
        triggerConfig.put("mediaId", automation.getExternalMediaId());
        triggerConfig.put("mediaLabel", automation.getMediaLabel());
        triggerConfig.put("matchType", automation.getMatchType());
        putIfPresent(triggerConfig, "keyword", automation.getKeyword());

        Map<String, String> publicReplyConfig = new TreeMap<>();
        publicReplyConfig.put("message", automation.getPublicReply());

        Map<String, String> privateReplyConfig = new TreeMap<>();
        privateReplyConfig.put("message", automation.getDirectMessage());
        putIfPresent(privateReplyConfig, "buttonText", automation.getDirectMessageLinkText());
        putIfPresent(privateReplyConfig, "buttonUrl", automation.getDirectMessageLinkUrl());

        return new AutomationDefinition(
                AutomationDefinition.CURRENT_SCHEMA_VERSION,
                new AutomationDefinition.TriggerDefinition(
                        AutomationDefinition.INSTAGRAM_COMMENT_RECEIVED,
                        AutomationDefinition.CURRENT_SCHEMA_VERSION,
                        triggerConfig),
                List.of(
                        new AutomationDefinition.StepDefinition(
                                "public-reply",
                                AutomationDefinition.INSTAGRAM_PUBLIC_COMMENT_REPLY,
                                AutomationDefinition.CURRENT_SCHEMA_VERSION,
                                publicReplyConfig),
                        new AutomationDefinition.StepDefinition(
                                "opening-dm",
                                AutomationDefinition.INSTAGRAM_PRIVATE_REPLY,
                                AutomationDefinition.CURRENT_SCHEMA_VERSION,
                                privateReplyConfig)));
    }

    public String toJson(AutomationDefinition definition) {
        validate(definition);
        return objectMapper.valueToTree(definition).toString();
    }

    public AutomationDefinition fromJson(String json) {
        try {
            AutomationDefinition definition = objectMapper.readValue(json, AutomationDefinition.class);
            validate(definition);
            return definition;
        } catch (JacksonException exception) {
            throw new IllegalStateException("The saved automation definition could not be read.", exception);
        }
    }

    private void validate(AutomationDefinition definition) {
        if (definition == null
                || definition.schemaVersion() != AutomationDefinition.CURRENT_SCHEMA_VERSION
                || definition.trigger() == null
                || definition.trigger().schemaVersion() != AutomationDefinition.CURRENT_SCHEMA_VERSION
                || definition.trigger().type() == null || definition.trigger().type().isBlank()
                || definition.steps() == null
                || definition.steps().isEmpty()) {
            throw new IllegalArgumentException("The automation definition is incomplete or unsupported.");
        }
        Set<String> stepIds = new HashSet<>();
        for (AutomationDefinition.StepDefinition step : definition.steps()) {
            if (step == null
                    || step.id() == null || step.id().isBlank()
                    || step.type() == null || step.type().isBlank()
                    || step.schemaVersion() != AutomationDefinition.CURRENT_SCHEMA_VERSION
                    || step.config() == null
                    || !stepIds.add(step.id())) {
                throw new IllegalArgumentException("The automation contains an invalid step.");
            }
        }
    }

    private String normalizedKeyword(InstagramCommentAutomationRequestDto request) {
        return "KEYWORD".equals(request.matchType()) ? normalized(request.keyword()) : null;
    }

    private String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void putIfPresent(Map<String, String> values, String key, String value) {
        if (value != null && !value.isBlank()) {
            values.put(key, value);
        }
    }
}
