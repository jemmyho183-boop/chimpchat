package com.chimpchat.api.channel.business;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class MetaInstagramBusiness {
    private static final String AUTHORIZATION_ENDPOINT = "https://www.facebook.com/dialog/oauth";
    private static final String GRAPH_ENDPOINT = "https://graph.facebook.com/v26.0";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private final String appId;
    private final String appSecret;
    private final String configId;
    private final String redirectUri;

    public MetaInstagramBusiness(
            ObjectMapper objectMapper,
            @Value("${app.oauth.instagram.app-id:}") String appId,
            @Value("${app.oauth.instagram.app-secret:}") String appSecret,
            @Value("${app.oauth.instagram.config-id:}") String configId,
            @Value("${app.oauth.instagram.redirect-uri:}") String redirectUri) {
        this.objectMapper = objectMapper;
        this.appId = appId;
        this.appSecret = appSecret;
        this.configId = configId;
        this.redirectUri = redirectUri;
    }

    public boolean isConfigured() {
        return isPresent(appId) && isPresent(appSecret)
                && isPresent(configId) && isPresent(redirectUri);
    }

    public String authorizationUrl(String state) {
        requireConfigured(appId, "META_APP_ID");
        requireConfigured(configId, "META_CONFIG_ID");
        requireConfigured(redirectUri, "META_REDIRECT_URI");

        return UriComponentsBuilder.fromUriString(AUTHORIZATION_ENDPOINT)
                .queryParam("client_id", appId)
                .queryParam("config_id", configId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("display", "popup")
                .queryParam("prompt", "login")
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
    }

    public MetaConnection exchangeCode(String code) {
        requireConfigured(appId, "META_APP_ID");
        requireConfigured(appSecret, "META_APP_SECRET");
        requireConfigured(redirectUri, "META_REDIRECT_URI");

        URI tokenUri = UriComponentsBuilder.fromUriString(GRAPH_ENDPOINT + "/oauth/access_token")
                .queryParam("client_id", appId)
                .queryParam("client_secret", appSecret)
                .queryParam("code", code)
                .queryParam("redirect_uri", redirectUri)
                .build()
                .encode()
                .toUri();
        String accessToken = requiredText(getJson(tokenUri), "access_token");

        URI businessUri = UriComponentsBuilder.fromUriString(GRAPH_ENDPOINT + "/me")
                .queryParam("fields", "client_business_id")
                .queryParam("access_token", accessToken)
                .build()
                .encode()
                .toUri();
        String businessId = requiredText(getJson(businessUri), "client_business_id");

        URI accountsUri = UriComponentsBuilder
                .fromUriString(GRAPH_ENDPOINT + "/me/assigned_instagram_accounts")
                .queryParam("fields", "id,username")
                .queryParam("access_token", accessToken)
                .build()
                .encode()
                .toUri();
        List<InstagramAccount> accounts = readAccounts(getJson(accountsUri));
        if (accounts.isEmpty()) {
            throw new ChannelException("No Instagram accounts were shared with this app.");
        }

        return new MetaConnection(businessId, accounts, accessToken);
    }

    private List<InstagramAccount> readAccounts(JsonNode response) {
        JsonNode data = response.path("data");
        if (!data.isArray()) {
            throw new ChannelException("Meta did not return the connected Instagram accounts.");
        }

        List<InstagramAccount> accounts = new ArrayList<>();
        for (JsonNode account : data) {
            accounts.add(new InstagramAccount(
                    requiredText(account, "id"),
                    requiredText(account, "username")));
        }
        return List.copyOf(accounts);
    }

    private JsonNode getJson(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ChannelException(metaError(response));
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ChannelException("Meta login was interrupted.", exception);
        } catch (IOException exception) {
            throw new ChannelException("Could not complete the Meta API request.", exception);
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
            throw new ChannelException("Meta response did not include " + field + ".");
        }
        return value.asText();
    }

    private void requireConfigured(String value, String setting) {
        if (!isPresent(value)) {
            throw new ChannelException("Instagram connection is not configured. Set " + setting + ".");
        }
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    public record InstagramAccount(String id, String username) {}

    public record MetaConnection(String clientBusinessId, List<InstagramAccount> accounts, String accessToken) {}

    public static class ChannelException extends RuntimeException {
        public ChannelException(String message) {
            super(message);
        }

        public ChannelException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
