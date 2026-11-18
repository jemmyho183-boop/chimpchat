package com.chimpchat.api.automation.business;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class InstagramCommentAutomationMetaBusiness {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper;
    private final String graphUri;

    public InstagramCommentAutomationMetaBusiness(
            ObjectMapper objectMapper,
            @Value("${app.oauth.instagram.graph-uri}") String graphUri) {
        this.objectMapper = objectMapper;
        this.graphUri = graphUri;
    }

    public MetaReply replyToComment(String commentId, String accessToken, String message) {
        URI uri = UriComponentsBuilder.fromUriString(graphUri)
                .pathSegment(commentId, "replies")
                .queryParam("message", message)
                .queryParam("access_token", accessToken)
                .build()
                .encode()
                .toUri();
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return postMessageRequest(request, "id", "reply");
    }

    public MetaReply sendPrivateReply(
            String instagramAccountId,
            String commentId,
            String accessToken,
            String message,
            String linkText,
            String linkUrl) {
        URI uri = UriComponentsBuilder.fromUriString(graphUri)
                .pathSegment(instagramAccountId, "messages")
                .build()
                .encode()
                .toUri();
        var payload = objectMapper.createObjectNode();
        payload.putObject("recipient").put("comment_id", commentId);
        String messageText = message;
        if (linkUrl != null && !linkUrl.isBlank()) {
            messageText += "\n\n" + linkText + ": " + linkUrl;
        }
        payload.putObject("message").put("text", messageText);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();
        return postMessageRequest(request, "message_id", "private reply");
    }

    public List<InstagramMedia> listMedia(String instagramAccountId, String accessToken) {
        URI uri = UriComponentsBuilder.fromUriString(graphUri)
                .pathSegment(instagramAccountId, "media")
                .queryParam("fields", "id,caption,media_type,thumbnail_url,media_url,permalink,timestamp")
                .queryParam("limit", 50)
                .queryParam("access_token", accessToken)
                .build()
                .encode()
                .toUri();
        JsonNode response = getJson(uri);
        JsonNode data = response.path("data");
        if (!data.isArray()) {
            throw new MetaRequestException("Meta did not return Instagram media.", false);
        }
        List<InstagramMedia> media = new ArrayList<>();
        for (JsonNode item : data) {
            String timestamp = optionalText(item, "timestamp");
            media.add(new InstagramMedia(
                    requiredText(item, "id"),
                    optionalText(item, "caption"),
                    optionalText(item, "media_type"),
                    optionalText(item, "thumbnail_url"),
                    optionalText(item, "media_url"),
                    optionalText(item, "permalink"),
                    timestamp == null ? null : parseTimestamp(timestamp)));
        }
        return List.copyOf(media);
    }

    private JsonNode getJson(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new MetaRequestException(metaError(response), false);
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new MetaRequestException("Meta API request was interrupted.", false, exception);
        } catch (IOException exception) {
            throw new MetaRequestException("Could not complete the Meta API request.", false, exception);
        }
    }

    private MetaReply postMessageRequest(HttpRequest request, String idField, String action) {
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new MetaRequestException("Meta could not send the " + action + ": " + metaError(response), false);
            }
            String replyId = optionalText(objectMapper.readTree(response.body()), idField);
            if (replyId == null) {
                throw new MetaRequestException(
                        "Meta did not confirm the " + action + ". Check Instagram before trying again.", true);
            }
            return new MetaReply(replyId);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new MetaRequestException(
                    "The " + action + " outcome is unknown. Check Instagram before trying again.", true, exception);
        } catch (IOException exception) {
            throw new MetaRequestException(
                    "The " + action + " outcome is unknown. Check Instagram before trying again.", true, exception);
        }
    }

    private String metaError(HttpResponse<String> response) {
        String message = "Meta API request failed with HTTP " + response.statusCode();
        JsonNode error = objectMapper.readTree(response.body()).path("error");
        String description = error.path("message").asText("");
        String code = error.path("code").asText("");
        String subcode = error.path("error_subcode").asText("");
        if (!description.isBlank()) {
            message += ": " + description;
        }
        if (!code.isBlank()) {
            message += " (code " + code
                    + (subcode.isBlank() ? "" : ", subcode " + subcode)
                    + ")";
        }
        return message;
    }

    private String requiredText(JsonNode json, String field) {
        JsonNode value = json.path(field);
        if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
            throw new MetaRequestException("Meta response did not include " + field + ".", false);
        }
        return value.asText();
    }

    private String optionalText(JsonNode json, String field) {
        JsonNode value = json.path(field);
        if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
            return null;
        }
        return value.asText();
    }

    private String textOrNull(JsonNode value) {
        return value.isMissingNode() || value.isNull() || value.asText().isBlank()
                ? null
                : value.asText();
    }

    private Instant parseTimestamp(String timestamp) {
        String normalized = timestamp.replaceFirst("([+-]\\d{2})(\\d{2})$", "$1:$2");
        return OffsetDateTime.parse(normalized).toInstant();
    }

    public record InstagramMedia(
            String id,
            String caption,
            String mediaType,
            String thumbnailUrl,
            String mediaUrl,
            String permalink,
            Instant timestamp) {}

    public record MetaReply(String id) {}

    public static class MetaRequestException extends RuntimeException {
        private final boolean outcomeUnknown;

        public MetaRequestException(String message, boolean outcomeUnknown) {
            super(message);
            this.outcomeUnknown = outcomeUnknown;
        }

        public MetaRequestException(String message, boolean outcomeUnknown, Throwable cause) {
            super(message, cause);
            this.outcomeUnknown = outcomeUnknown;
        }

        public boolean isOutcomeUnknown() {
            return outcomeUnknown;
        }
    }
}
