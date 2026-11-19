package com.chimpchat.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.chimpchat.api.automation.business.InstagramCommentAutomationMetaBusiness;
import com.chimpchat.api.channel.business.TokenEncryptionBusiness;
import com.chimpchat.api.auth.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.frontend-url=http://localhost:3000",
        "app.auth.google.enabled=false",
        "app.oauth.instagram.app-secret=test-meta-app-secret",
        "app.webhooks.instagram.verify-token=test-webhook-verify-token"
})
class InstagramCommentAutomationFollowUpTests {
    private static final String WEBHOOK = "/api/v1/channels/instagram/webhook";
    private static final String APP_SECRET = "test-meta-app-secret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private InstagramCommentAutomationMetaBusiness metaBusiness;

    @MockitoBean
    private TokenEncryptionBusiness tokenEncryptionBusiness;

    @Test
    void sendsLinkButtonInTheInitialPrivateMessage() throws Exception {
        TestAccount account = createAccount();
        when(tokenEncryptionBusiness.decrypt("encrypted-token")).thenReturn("access-token");
        when(metaBusiness.replyToComment(anyString(), anyString(), anyString()))
                .thenReturn(new InstagramCommentAutomationMetaBusiness.MetaReply("public-reply-id"));
        when(metaBusiness.sendPrivateReply(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new InstagramCommentAutomationMetaBusiness.MetaReply("private-reply-id"));

        postWebhook(commentPayload(account.accountId(), account.mediaId(), "comment-id"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        awaitLinkButtonStatus("SENT", account.connectionId());
        String definitionSnapshot = jdbcTemplate.queryForObject(
                """
                SELECT execution.definition_snapshot
                FROM instagram_comment_automation_executions execution
                JOIN instagram_comments comment ON comment.id = execution.comment_id
                WHERE comment.connection_id = ? AND comment.external_comment_id = 'comment-id'
                """,
                String.class,
                account.connectionId());
        assertTrue(definitionSnapshot.contains("INSTAGRAM_COMMENT_RECEIVED"));
        assertTrue(definitionSnapshot.contains("INSTAGRAM_PUBLIC_COMMENT_REPLY"));
        assertEquals(2, jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM automation_step_executions step_execution
                JOIN instagram_comment_automation_executions execution ON execution.id = step_execution.execution_id
                JOIN instagram_comments comment ON comment.id = execution.comment_id
                WHERE comment.connection_id = ? AND comment.external_comment_id = 'comment-id'
                    AND step_execution.status = 'SENT'
                """,
                Integer.class,
                account.connectionId()));
        verify(metaBusiness, times(1)).sendPrivateReply(
                account.accountId(), "comment-id", "access-token", "Thanks for asking!", "Shop now",
                "https://example.test/shop");
        verify(metaBusiness, times(0)).sendLinkButtonFollowUp(
                anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void recordsStepOutcomesIndependentlyAndContinuesAfterAReplyFailure() throws Exception {
        TestAccount account = createAccount();
        when(tokenEncryptionBusiness.decrypt("encrypted-token")).thenReturn("access-token");
        when(metaBusiness.replyToComment(anyString(), anyString(), anyString()))
                .thenThrow(new InstagramCommentAutomationMetaBusiness.MetaRequestException(
                        "Public reply failed.", false));
        when(metaBusiness.sendPrivateReply(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new InstagramCommentAutomationMetaBusiness.MetaReply("private-reply-id"));

        postWebhook(commentPayload(account.accountId(), account.mediaId(), "comment-id"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        awaitStepStatus("SENT", "opening-dm", account.connectionId());

        assertEquals("FAILED", jdbcTemplate.queryForObject(
                """
                SELECT step_execution.status
                FROM automation_step_executions step_execution
                JOIN instagram_comment_automation_executions execution ON execution.id = step_execution.execution_id
                JOIN instagram_comments comment ON comment.id = execution.comment_id
                WHERE comment.connection_id = ? AND comment.external_comment_id = 'comment-id'
                    AND step_execution.step_id = 'public-reply'
                """,
                String.class,
                account.connectionId()));
        verify(metaBusiness, times(1)).sendPrivateReply(
                account.accountId(), "comment-id", "access-token", "Thanks for asking!", "Shop now",
                "https://example.test/shop");
    }

    private void awaitStepStatus(String expected, String stepId, UUID connectionId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        String status = null;
        while (System.currentTimeMillis() < deadline) {
            status = jdbcTemplate.query(
                    """
                    SELECT step_execution.status
                    FROM automation_step_executions step_execution
                    JOIN instagram_comment_automation_executions execution ON execution.id = step_execution.execution_id
                    JOIN instagram_comments comment ON comment.id = execution.comment_id
                    WHERE comment.connection_id = ? AND comment.external_comment_id = 'comment-id'
                        AND step_execution.step_id = ?
                    """,
                    resultSet -> resultSet.next() ? resultSet.getString(1) : null,
                    connectionId,
                    stepId);
            if (expected.equals(status)) {
                return;
            }
            Thread.sleep(20);
        }
        assertEquals(expected, status);
    }

    private void awaitLinkButtonStatus(String expected, UUID connectionId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        String status = null;
        while (System.currentTimeMillis() < deadline) {
            status = jdbcTemplate.query(
                    """
                    SELECT execution.link_button_status
                    FROM instagram_comment_automation_executions execution
                    JOIN instagram_comments comment ON comment.id = execution.comment_id
                    WHERE comment.connection_id = ? AND comment.external_comment_id = 'comment-id'
                    """,
                    resultSet -> resultSet.next() ? resultSet.getString(1) : null,
                    connectionId);
            if (expected.equals(status)) {
                return;
            }
            Thread.sleep(20);
        }
        assertEquals(expected, status);
    }

    private TestAccount createAccount() {
        UserEntity user = new UserEntity("follow-up-" + UUID.randomUUID() + "@example.com");
        Instant now = Instant.now();
        jdbcTemplate.update(
                "INSERT INTO app_users (id, email, created_at, updated_at) VALUES (?, ?, ?, ?)",
                user.getId(), user.getEmail(), Timestamp.from(now), Timestamp.from(now));
        UUID workspaceId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO workspaces (id, owner_user_id, name, created_at) VALUES (?, ?, ?, ?)",
                workspaceId, user.getId(), "Follow-up test workspace", Timestamp.from(now));
        UUID connectionId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO channel_connections
                    (id, workspace_id, provider, external_business_id, encrypted_access_token, created_at, updated_at)
                VALUES (?, ?, 'INSTAGRAM', ?, 'encrypted-token', ?, ?)
                """,
                connectionId, workspaceId, "business-" + UUID.randomUUID(), Timestamp.from(now), Timestamp.from(now));
        String accountId = "ig-" + UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO channel_accounts
                    (id, connection_id, external_account_id, username, profile_picture_url)
                VALUES (?, ?, ?, 'test-account', NULL)
                """,
                UUID.randomUUID(), connectionId, accountId);
        String mediaId = "media-" + UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO instagram_comment_automations
                    (id, connection_id, instagram_account_id, external_media_id, media_label, match_type,
                     public_reply, direct_message, direct_message_link_text, direct_message_link_url,
                     enabled, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'Test post', 'ANY_COMMENT', 'Thanks for commenting!',
                        'Thanks for asking!', 'Shop now', 'https://example.test/shop', TRUE, ?, ?)
                """,
                UUID.randomUUID(), connectionId, accountId, mediaId, Timestamp.from(now), Timestamp.from(now));
        return new TestAccount(connectionId, accountId, mediaId);
    }

    private org.springframework.test.web.servlet.ResultActions postWebhook(String payload) throws Exception {
        byte[] body = payload.getBytes(StandardCharsets.UTF_8);
        return mockMvc.perform(post(WEBHOOK)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", signature(body))
                .content(body));
    }

    private String signature(byte[] payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(APP_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload));
    }

    private String commentPayload(String accountId, String mediaId, String commentId) {
        return """
                {"object":"instagram","entry":[{"id":"%s","changes":[{"field":"comments","value":{"id":"%s","from":{"id":"commenter-id","username":"commenter"},"text":"I want the link","media":{"id":"%s","media_product_type":"FEED"}}}]}]}
                """.formatted(accountId, commentId, mediaId);
    }

    private String messagePayload(String accountId, String commenterId, String replyToMessageId) {
        return messagePayload(accountId, commenterId, replyToMessageId, Instant.now().toEpochMilli());
    }

    private String messagePayload(
            String accountId, String commenterId, String replyToMessageId, long timestamp) {
        return """
                {"object":"instagram","entry":[{"id":"%s","messaging":[{"sender":{"id":"%s"},"recipient":{"id":"%s"},"timestamp":%d,"message":{"mid":"incoming-message-id","text":"Please send it","reply_to":{"mid":"%s"}}}]}]}
                """.formatted(accountId, commenterId, accountId, timestamp, replyToMessageId);
    }

    private record TestAccount(UUID connectionId, String accountId, String mediaId) {}
}
