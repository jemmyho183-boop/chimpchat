package com.chimpchat.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.chimpchat.api.auth.business.AppUserPrincipal;
import com.chimpchat.api.auth.entity.UserEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.MediaType;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(
		properties = {
				"app.frontend-url=http://localhost:3000",
				"app.auth.google.enabled=false",
				"app.oauth.instagram.app-secret=test-meta-app-secret",
				"app.webhooks.instagram.verify-token=test-webhook-verify-token"
		})
class ChimpchatapiApplicationTests {
	private static final String INSTAGRAM_WEBHOOK = "/api/v1/channels/instagram/webhook";
	private static final String INSTAGRAM_AUTOMATIONS = "/api/v1/channels/instagram/automations";
	private static final String META_APP_SECRET = "test-meta-app-secret";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void healthEndpointRemainsPublic() throws Exception {
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk());
	}

	@Test
	void authOptionsDoNotEnableUnconfiguredProviders() throws Exception {
		String response = mockMvc.perform(get("/api/v1/auth/options"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertTrue(response.contains("\"googleEnabled\":false"), response);
		assertTrue(response.contains("\"emailEnabled\":false"), response);
	}

	@Test
	void googleStartRouteIsPublicAndReportsDisabledConfiguration() throws Exception {
		mockMvc.perform(get("/api/auth/google/start"))
				.andExpect(status().is3xxRedirection())
				.andExpect(result -> assertTrue(
						result.getResponse().getRedirectedUrl().contains("auth_error=google_sign_in_unavailable")));
	}

	@Test
	void appAndChannelRoutesRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/app/bootstrap"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/channels/instagram/oauth/start"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get(INSTAGRAM_AUTOMATIONS))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void instagramOAuthCallbackUsesSessionStateInsteadOfRequiringAuthentication() throws Exception {
		String state = "one-time-state";
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("instagram_oauth_states", Map.of(
				state, UUID.randomUUID() + ":" + System.currentTimeMillis()));

		mockMvc.perform(get("/api/v1/channels/instagram/oauth/callback")
						.session(session)
						.param("state", state)
						.param("error", "access_denied"))
				.andExpect(status().is3xxRedirection())
				.andExpect(result -> assertTrue(
						result.getResponse().getRedirectedUrl().contains(
								"/auth/popup-result?provider=instagram&status=error&error=instagram_login_cancelled")));
	}

	@Test
	void instagramWebhookReturnsMetaChallengeForConfiguredVerifyToken() throws Exception {
		mockMvc.perform(get(INSTAGRAM_WEBHOOK)
						.param("hub.mode", "subscribe")
						.param("hub.verify_token", "test-webhook-verify-token")
						.param("hub.challenge", "test-challenge"))
				.andExpect(status().isOk())
				.andExpect(content().string("test-challenge"));
	}

	@Test
	void instagramWebhookRejectsIncorrectVerifyToken() throws Exception {
		mockMvc.perform(get(INSTAGRAM_WEBHOOK)
						.param("hub.mode", "subscribe")
						.param("hub.verify_token", "wrong-token")
						.param("hub.challenge", "test-challenge"))
				.andExpect(status().isForbidden());
	}

	@Test
	void instagramWebhookRejectsInvalidSignature() throws Exception {
		mockMvc.perform(post(INSTAGRAM_WEBHOOK)
						.contentType("application/json")
						.header("X-Hub-Signature-256", "sha256=" + "0".repeat(64))
						.content("{\"object\":\"instagram\",\"entry\":[]}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void instagramWebhookAcceptsSignedCommentNotification() throws Exception {
		byte[] payload = """
				{"object":"instagram","entry":[{"id":"ig-account-id","changes":[{"field":"comments","value":{"id":"comment-id","text":"test comment","media":{"id":"media-id"}}}]}]}
				""".getBytes(StandardCharsets.UTF_8);

		mockMvc.perform(post(INSTAGRAM_WEBHOOK)
						.contentType("application/json")
						.header("X-Hub-Signature-256", signature(payload))
						.content(payload))
				.andExpect(status().isOk())
				.andExpect(content().string("EVENT_RECEIVED"));
	}

	@Test
	void instagramCommentWebhooksUpsertCommentsForAutomation() throws Exception {
		TestWorkspace owner = createWorkspace("comment-owner");
		String firstPayload = commentPayload(owner.accountId(), "comment-id", "original comment");
		String updatedPayload = commentPayload(owner.accountId(), "comment-id", "edited comment");

		postWebhook(firstPayload).andExpect(status().isOk());
		postWebhook(firstPayload).andExpect(status().isOk());
		postWebhook(updatedPayload).andExpect(status().isOk());

		assertEquals(1, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM instagram_comments WHERE connection_id = ? AND external_comment_id = ?",
				Integer.class,
				owner.connectionId(),
				"comment-id"));
		assertEquals("edited comment", jdbcTemplate.queryForObject(
				"SELECT comment_text FROM instagram_comments WHERE connection_id = ? AND external_comment_id = ?",
				String.class,
				owner.connectionId(),
				"comment-id"));
	}

	@Test
	void instagramCommentRepliesAreNotTreatedAsNewAutomationTriggers() throws Exception {
		TestWorkspace owner = createWorkspace("self-reply-owner");
		String automationRequest = """
				{
					"accountId":"%s",
					"mediaId":"selected-media-id",
					"mediaLabel":"Selected post",
					"matchType":"ANY_COMMENT",
					"publicReply":"Thanks for your comment!",
					"directMessage":"Thanks for reaching out!",
					"enabled":true
				}
				""".formatted(owner.accountId());

		mockMvc.perform(post(INSTAGRAM_AUTOMATIONS)
						.with(asUser(owner.user()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(automationRequest))
				.andExpect(status().isCreated());

		String replyText = "Thanks for your comment!";
		postWebhook(commentPayload(
				owner.accountId(),
				"bot-reply-id",
				owner.accountId(),
				replyText,
				"selected-media-id"))
				.andExpect(status().isOk());
		postWebhook(commentReplyPayload(
				owner.accountId(),
				"user-reply-id",
				"another-commenter-id",
				replyText,
				"selected-media-id",
				"original-comment-id"))
				.andExpect(status().isOk());

		assertEquals(0, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM instagram_comments WHERE external_comment_id IN (?, ?)",
				Integer.class,
				"bot-reply-id",
				"user-reply-id"));
		assertEquals(0, jdbcTemplate.queryForObject(
				"""
				SELECT COUNT(*) FROM instagram_comment_automation_executions execution
				JOIN instagram_comment_automations automation ON automation.id = execution.automation_id
				WHERE automation.connection_id = ?
				""",
				Integer.class,
				owner.connectionId()));
	}

	@Test
	void instagramCommentAutomationIsScopedToPostAndKeywordAndDoesNotRepeat() throws Exception {
		TestWorkspace owner = createWorkspace("automation-owner");
		TestWorkspace otherOwner = createWorkspace("other-automation-owner");
		String automationRequest = """
				{
					"accountId":"%s",
					"mediaId":"selected-media-id",
					"mediaLabel":"Selected post",
					"matchType":"KEYWORD",
					"keyword":"BUY",
					"publicReply":"Thanks for asking!",
					"directMessage":"Here is the link: https://example.test",
					"enabled":true
				}
				""".formatted(owner.accountId());

		mockMvc.perform(post(INSTAGRAM_AUTOMATIONS)
						.with(asUser(owner.user()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(automationRequest))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.mediaId").value("selected-media-id"))
				.andExpect(jsonPath("$.matchType").value("KEYWORD"))
				.andExpect(jsonPath("$.enabled").value(true));

		mockMvc.perform(get(INSTAGRAM_AUTOMATIONS).with(asUser(otherOwner.user())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
		mockMvc.perform(post(INSTAGRAM_AUTOMATIONS)
						.with(asUser(owner.user()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(automationRequest))
				.andExpect(status().isConflict());
		mockMvc.perform(post(INSTAGRAM_AUTOMATIONS)
						.with(asUser(otherOwner.user()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(automationRequest))
				.andExpect(status().isNotFound());

		postWebhook(commentPayload(
				owner.accountId(), "non-matching-comment", "Please help", "selected-media-id"))
				.andExpect(status().isOk());
		postWebhook(commentPayload(
				owner.accountId(), "wrong-post-comment", "I want to buy", "another-media-id"))
				.andExpect(status().isOk());
		String matchingComment = commentPayload(
				owner.accountId(), "matching-comment", "I want to buy", "selected-media-id");
		postWebhook(matchingComment).andExpect(status().isOk());
		postWebhook(matchingComment).andExpect(status().isOk());
		awaitAutomationExecutionCount(1);
		Thread.sleep(100);
		assertEquals(1, automationExecutionCount());
		assertEquals(1, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM instagram_comment_automation_executions"
						+ " WHERE public_reply_status = 'FAILED' AND direct_message_status = 'FAILED'",
				Integer.class));
	}

	private int automationExecutionCount() {
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM instagram_comment_automation_executions", Integer.class);
	}

	private void awaitAutomationExecutionCount(int expected) throws InterruptedException {
		long deadline = System.currentTimeMillis() + 3000;
		while (automationExecutionCount() != expected && System.currentTimeMillis() < deadline) {
			Thread.sleep(20);
		}
		assertEquals(expected, automationExecutionCount());
	}

	@Test
	void emailLinkRequestFailsClearlyWhenEmailIsNotConfigured() throws Exception {
		mockMvc.perform(post("/api/v1/auth/email-links")
						.contentType("application/json")
						.content("{\"email\":\"person@example.com\"}")
						.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().isServiceUnavailable());
	}

	private String signature(byte[] payload) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(META_APP_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		return "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload));
	}

	private org.springframework.test.web.servlet.ResultActions postWebhook(String payload) throws Exception {
		byte[] body = payload.getBytes(StandardCharsets.UTF_8);
		return mockMvc.perform(post(INSTAGRAM_WEBHOOK)
				.contentType(MediaType.APPLICATION_JSON)
				.header("X-Hub-Signature-256", signature(body))
				.content(body));
	}

	private String commentPayload(String accountId, String commentId, String text) {
		return commentPayload(accountId, commentId, text, "media-id");
	}

	private String commentPayload(String accountId, String commentId, String text, String mediaId) {
		return commentPayload(accountId, commentId, "commenter-id", text, mediaId);
	}

	private String commentPayload(
			String accountId, String commentId, String commenterId, String text, String mediaId) {
		return """
				{"object":"instagram","entry":[{"id":"%s","time":1731456000,"changes":[{"field":"comments","value":{"id":"%s","from":{"id":"%s","username":"commenter"},"text":"%s","media":{"id":"%s","media_product_type":"FEED"}}}]}]}
				""".formatted(accountId, commentId, commenterId, text, mediaId);
	}

	private String commentReplyPayload(
			String accountId,
			String commentId,
			String commenterId,
			String text,
			String mediaId,
			String parentCommentId) {
		return """
				{"object":"instagram","entry":[{"id":"%s","time":1731456000,"changes":[{"field":"comments","value":{"id":"%s","from":{"id":"%s","username":"commenter"},"text":"%s","media":{"id":"%s","media_product_type":"FEED"},"parent_id":"%s"}}]}]}
				""".formatted(accountId, commentId, commenterId, text, mediaId, parentCommentId);
	}

	private TestWorkspace createWorkspace(String emailPrefix) {
		UserEntity user = new UserEntity(emailPrefix + "-" + UUID.randomUUID() + "@example.com");
		Instant now = Instant.now();
		jdbcTemplate.update(
				"INSERT INTO app_users (id, email, created_at, updated_at) VALUES (?, ?, ?, ?)",
				user.getId(), user.getEmail(), Timestamp.from(now), Timestamp.from(now));
		UUID workspaceId = UUID.randomUUID();
		jdbcTemplate.update(
				"INSERT INTO workspaces (id, owner_user_id, name, created_at) VALUES (?, ?, ?, ?)",
				workspaceId, user.getId(), "Test workspace", Timestamp.from(now));
		UUID connectionId = UUID.randomUUID();
		jdbcTemplate.update(
				"""
				INSERT INTO channel_connections
					(id, workspace_id, provider, external_business_id, encrypted_access_token, created_at, updated_at)
				VALUES (?, ?, 'INSTAGRAM', ?, ?, ?, ?)
				""",
				connectionId, workspaceId, "business-" + UUID.randomUUID(), "test-encrypted-token",
				Timestamp.from(now), Timestamp.from(now));
		String accountId = "ig-" + UUID.randomUUID();
		jdbcTemplate.update(
				"""
				INSERT INTO channel_accounts
					(id, connection_id, external_account_id, username, profile_picture_url)
				VALUES (?, ?, ?, ?, ?)
				""",
				UUID.randomUUID(), connectionId, accountId, "test-account", null);
		return new TestWorkspace(user, connectionId, accountId);
	}

	private RequestPostProcessor asUser(UserEntity user) {
		AppUserPrincipal principal = new AppUserPrincipal(user);
		return authentication(new UsernamePasswordAuthenticationToken(
				principal, null, principal.getAuthorities()));
	}

	private record TestWorkspace(UserEntity user, UUID connectionId, String accountId) {}

}
