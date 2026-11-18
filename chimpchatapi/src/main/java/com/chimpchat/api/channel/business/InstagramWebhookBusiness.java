package com.chimpchat.api.channel.business;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.chimpchat.api.automation.business.InstagramDirectMessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class InstagramWebhookBusiness {
    private static final Logger logger = LoggerFactory.getLogger(InstagramWebhookBusiness.class);
    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final String COMMENTS_FIELD = "comments";
    private static final String LIVE_COMMENTS_FIELD = "live_comments";
    private static final String INSTAGRAM_OBJECT = "instagram";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final InstagramCommentBusiness commentBusiness;
    private final ApplicationEventPublisher eventPublisher;
    private final String appSecret;
    private final String verifyToken;

    public InstagramWebhookBusiness(
            ObjectMapper objectMapper,
            InstagramCommentBusiness commentBusiness,
            ApplicationEventPublisher eventPublisher,
            @Value("${app.oauth.instagram.app-secret:}") String appSecret,
            @Value("${app.webhooks.instagram.verify-token:}") String verifyToken) {
        this.objectMapper = objectMapper;
        this.commentBusiness = commentBusiness;
        this.eventPublisher = eventPublisher;
        this.appSecret = appSecret;
        this.verifyToken = verifyToken;
    }

    public boolean isConfigured() {
        return isPresent(appSecret) && isPresent(verifyToken);
    }

    public boolean verifiesSubscription(String mode, String receivedToken) {
        return isConfigured()
                && "subscribe".equals(mode)
                && receivedToken != null
                && MessageDigest.isEqual(
                        verifyToken.getBytes(StandardCharsets.UTF_8),
                        receivedToken.getBytes(StandardCharsets.UTF_8));
    }

    public boolean verifiesSignature(byte[] payload, String signature) {
        if (!isPresent(appSecret) || signature == null
                || !signature.startsWith(SIGNATURE_PREFIX)
                || signature.length() != SIGNATURE_PREFIX.length() + 64) {
            return false;
        }

        String suppliedHex = signature.substring(SIGNATURE_PREFIX.length());
        if (!suppliedHex.matches("[0-9a-fA-F]{64}")) {
            return false;
        }
        String expectedHex = HexFormat.of().formatHex(computeSignature(payload));
        return MessageDigest.isEqual(
                expectedHex.getBytes(StandardCharsets.US_ASCII),
                suppliedHex.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }

    public void processCommentNotifications(byte[] payload) throws IOException {
        JsonNode root = objectMapper.readTree(payload);
        if (!INSTAGRAM_OBJECT.equals(root.path("object").asText())) {
            return;
        }

        JsonNode entries = root.path("entry");
        if (!entries.isArray()) {
            throw new IllegalArgumentException("Instagram webhook payload must contain an entry array.");
        }
        for (JsonNode entry : entries) {
            String accountId = textOrNull(entry.path("id"));
            JsonNode changes = entry.path("changes");
            if (changes.isArray()) {
                for (JsonNode change : changes) {
                    processCommentChange(accountId, change);
                }
            } else if (entry.has("field")) {
                processCommentChange(accountId, entry);
            }
            JsonNode messaging = entry.path("messaging");
            if (messaging.isArray()) {
                for (JsonNode messageEvent : messaging) {
                    processMessage(accountId, messageEvent);
                }
            }
        }
    }

    private void processMessage(String accountId, JsonNode messageEvent) {
        JsonNode sender = messageEvent.path("sender");
        JsonNode message = messageEvent.path("message");
        String commenterId = textOrNull(sender.path("id"));
        long timestamp = messageEvent.path("timestamp").asLong(0L);
        if (accountId == null || commenterId == null
                || accountId.equals(commenterId)
                || !message.isObject()
                || message.path("is_echo").asBoolean(false)
                || message.path("is_deleted").asBoolean(false)) {
            return;
        }
        if (timestamp <= 0) {
            logger.warn("Received an Instagram message notification without a valid timestamp.");
            return;
        }

        eventPublisher.publishEvent(new InstagramDirectMessageReceivedEvent(
                accountId,
                commenterId,
                textOrNull(message.path("reply_to").path("mid")),
                Instant.ofEpochMilli(timestamp)));
    }

    private void processCommentChange(String accountId, JsonNode change) {
        String field = change.path("field").asText();
        if (!COMMENTS_FIELD.equals(field) && !LIVE_COMMENTS_FIELD.equals(field)) {
            return;
        }

        JsonNode value = change.path("value");
        if (value.isArray()) {
            for (JsonNode comment : value) {
                receiveComment(accountId, comment);
            }
        } else if (value.isObject()) {
            receiveComment(accountId, value);
        }
    }

    private void receiveComment(String accountId, JsonNode comment) {
        String commentId = textOrNull(comment.path("id"));
        if (accountId == null || commentId == null) {
            logger.warn("Received an Instagram comment notification without an account or comment ID.");
            return;
        }

        JsonNode media = comment.path("media");
        JsonNode from = comment.path("from");
        commentBusiness.receiveComment(
                accountId,
                commentId,
                textOrNull(media.path("id")),
                textOrNull(comment.path("parent_id")),
                textOrNull(from.path("id")),
                textOrNull(from.path("username")),
                textOrNull(comment.path("text")),
                textOrNull(media.path("media_product_type")),
                textOrNull(comment.path("verb")));
    }

    private byte[] computeSignature(byte[] payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable.", exception);
        }
    }

    private String textOrNull(JsonNode value) {
        return value.isMissingNode() || value.isNull() || value.asText().isBlank()
                ? null
                : value.asText();
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
