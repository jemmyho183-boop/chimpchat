package com.chimpchat.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
	private static final String META_APP_SECRET = "test-meta-app-secret";

	@Autowired
	private MockMvc mockMvc;

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

}
