package com.chimpchat.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(
		properties = {
				"app.frontend-url=http://localhost:3000",
				"app.auth.google.enabled=false"
		})
class ChimpchatapiApplicationTests {

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
	void emailLinkRequestFailsClearlyWhenEmailIsNotConfigured() throws Exception {
		mockMvc.perform(post("/api/v1/auth/email-links")
						.contentType("application/json")
						.content("{\"email\":\"person@example.com\"}")
						.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
				.andExpect(status().isServiceUnavailable());
	}

}
