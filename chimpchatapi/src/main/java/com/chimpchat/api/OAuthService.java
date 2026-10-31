package com.chimpchat.api;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class OAuthService {
    private final HttpClient http = HttpClient.newHttpClient();
    @Value("${app.oauth.instagram.client-id}") String instagramId;
    @Value("${app.oauth.instagram.client-secret}") String instagramSecret;
    @Value("${app.oauth.instagram.config-id}") String instagramConfigId;
    @Value("${app.oauth.instagram.redirect-uri}") String instagramRedirect;
    @Value("${app.oauth.facebook.client-id}") String facebookId;
    @Value("${app.oauth.facebook.client-secret}") String facebookSecret;
    @Value("${app.oauth.facebook.config-id}") String facebookConfigId;
    @Value("${app.oauth.facebook.redirect-uri}") String facebookRedirect;

    String authorizationUrl(String provider, String state) {
        if ("instagram".equals(provider)) return UriComponentsBuilder.fromUriString("https://www.facebook.com/dialog/oauth")
                .queryParam("client_id", instagramId).queryParam("config_id", instagramConfigId).queryParam("display", "popup")
                .queryParam("override_default_response_type", "true").queryParam("redirect_uri", instagramRedirect).queryParam("response_type", "code")
                .queryParam("scope", "instagram_basic,instagram_manage_comments,instagram_manage_messages")
                .queryParam("state", state).queryParam("prompt", "login").build().toUriString();
        if ("facebook".equals(provider)) return UriComponentsBuilder.fromUriString("https://www.facebook.com/dialog/oauth")
                .queryParam("client_id", facebookId).queryParam("config_id", facebookConfigId).queryParam("display", "popup")
                .queryParam("override_default_response_type", "true").queryParam("redirect_uri", facebookRedirect).queryParam("response_type", "code")
                .queryParam("state", state).queryParam("prompt", "login").build().toUriString();
        throw new IllegalArgumentException("Unsupported provider");
    }

    Map<String, Object> exchange(String provider, String code) throws Exception {
        String id = "instagram".equals(provider) ? instagramId : facebookId;
        String secret = "instagram".equals(provider) ? instagramSecret : facebookSecret;
        String redirect = "instagram".equals(provider) ? instagramRedirect : facebookRedirect;
        String endpoint = "https://graph.facebook.com/v23.0/oauth/access_token";
        String body = "client_id=" + enc(id) + "&client_secret=" + enc(secret) + "&redirect_uri=" + enc(redirect) + "&code=" + enc(code)
                + ("facebook".equals(provider) ? "&grant_type=authorization_code" : "&grant_type=authorization_code");
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> result = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (result.statusCode() >= 400) throw new IllegalStateException("Meta token exchange failed");
        // TODO: persist the returned token encrypted and associate it with the authenticated ChimpChat user.
        return Map.of("provider", provider, "metaResponse", result.body());
    }

    private String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
