package com.chimpchat.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"META_APP_ID=1234567890",
				"META_APP_SECRET=test-secret",
				"META_INSTAGRAM_CONFIG_ID=test-config",
				"META_REDIRECT_URI=https://gauntlet-sliver-headfirst.ngrok-free.dev/api/auth/instagram/callback"
		})
class ChimpchatapiApplicationTests {

	@LocalServerPort
	private int port;

	@Test
	void healthEndpointReturnsServiceStatus() throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:" + port + "/api/health"))
				.GET()
				.build();
		HttpResponse<String> response = HttpClient.newHttpClient()
				.send(request, HttpResponse.BodyHandlers.ofString());

		assertEquals(200, response.statusCode());
		assertTrue(response.body().contains("\"status\":\"UP\""));
		assertTrue(response.body().contains("\"service\":\"chimpchatapi\""));
	}

	@Test
	void instagramLoginRedirectUsesUnifiedLoginConfiguration() throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:" + port + "/api/auth/instagram"))
				.GET()
				.build();
		HttpResponse<Void> response = HttpClient.newHttpClient()
				.send(request, HttpResponse.BodyHandlers.discarding());

		assertEquals(302, response.statusCode());
		String location = response.headers().firstValue("location").orElseThrow();
		assertTrue(location.startsWith("https://www.facebook.com/dialog/oauth?"));
		String decodedLocation = URLDecoder.decode(location, StandardCharsets.UTF_8);
		assertTrue(decodedLocation.contains("client_id=1234567890"));
		assertTrue(decodedLocation.contains("config_id=test-config"));
		assertTrue(decodedLocation.contains("display=popup"));
		assertTrue(decodedLocation.contains("prompt=login"));
		assertTrue(decodedLocation.contains("state="));
	}

}
