package com.chimpchat.api.channel.business;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class MetaInstagramBusiness {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper;
    private final String appId;
    private final String appSecret;
    private final String configId;
    private final String redirectUri;
    private final String authorizationUri;
    private final String graphUri;

    public MetaInstagramBusiness(
            ObjectMapper objectMapper,
            @Value("${app.oauth.instagram.app-id:}") String appId,
            @Value("${app.oauth.instagram.app-secret:}") String appSecret,
            @Value("${app.oauth.instagram.config-id:}") String configId,
            @Value("${app.oauth.instagram.redirect-uri:}") String redirectUri,
            @Value("${app.oauth.instagram.authorization-uri}") String authorizationUri,
            @Value("${app.oauth.instagram.graph-uri}") String graphUri) {
        this.objectMapper = objectMapper;
        this.appId = appId;
        this.appSecret = appSecret;
        this.configId = configId;
        this.redirectUri = redirectUri;
        this.authorizationUri = authorizationUri;
        this.graphUri = graphUri;
    }

    public boolean isConfigured() {
        return isPresent(appId) && isPresent(appSecret)
                && isPresent(configId) && isPresent(redirectUri);
    }

    public String authorizationUrl(String state) {
        requireConfigured(appId, "META_APP_ID");
        requireConfigured(configId, "META_CONFIG_ID");
        requireConfigured(redirectUri, "FRONTEND_URL or LOCAL_FRONTEND_URL");

        return UriComponentsBuilder.fromUriString(authorizationUri)
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
        requireConfigured(redirectUri, "FRONTEND_URL or LOCAL_FRONTEND_URL");

        URI tokenUri = UriComponentsBuilder.fromUriString(graphUri + "/oauth/access_token")
                .queryParam("client_id", appId)
                .queryParam("client_secret", appSecret)
                .queryParam("code", code)
                .queryParam("redirect_uri", redirectUri)
                .build()
                .encode()
                .toUri();
        String accessToken = requiredText(getJson(tokenUri), "access_token");

        URI businessUri = UriComponentsBuilder.fromUriString(graphUri + "/me")
                .queryParam("fields", "client_business_id")
                .queryParam("access_token", accessToken)
                .build()
                .encode()
                .toUri();
        String businessId = requiredText(getJson(businessUri), "client_business_id");

        URI accountsUri = UriComponentsBuilder
                .fromUriString(graphUri + "/me/assigned_instagram_accounts")
                .queryParam("fields", "id,username,profile_picture_url")
                .queryParam("access_token", accessToken)
                .build()
                .encode()
                .toUri();
        List<InstagramAccount> accounts = readAccounts(getJson(accountsUri));
        if (accounts.isEmpty()) {
            throw new ChannelException("No Instagram accounts were shared with this app.");
        }
        for (InstagramAccount account : accounts) {
            subscribeToCommentWebhooks(accessToken, account);
        }

        return new MetaConnection(businessId, accounts, accessToken);
    }

    private void subscribeToCommentWebhooks(String accessToken, InstagramAccount account) {
        URI uri = UriComponentsBuilder.fromUriString(graphUri)
                .pathSegment(account.id(), "subscribed_apps")
                .build()
                .encode()
                .toUri();
        String body = "subscribed_fields=comments&access_token="
                + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new WebhookSubscriptionException(metaError(response));
            }
            if (!objectMapper.readTree(response.body()).path("success").asBoolean(false)) {
                throw new WebhookSubscriptionException(
                        "Meta did not confirm comment notifications for Instagram account " + account.id() + ".");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new WebhookSubscriptionException(
                    "Instagram comment notification setup was interrupted.", exception);
        } catch (IOException exception) {
            throw new WebhookSubscriptionException(
                    "Could not subscribe the Instagram account to comment notifications.", exception);
        }
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
                    requiredText(account, "username"),
                    optionalText(account, "profile_picture_url")));
        }
        return List.copyOf(accounts);
    }

    private JsonNode getJson(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
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

    private String optionalText(JsonNode json, String field) {
        JsonNode value = json.path(field);
        if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
            return null;
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

    public record InstagramAccount(String id, String username, String profilePictureUrl) {}

    public record MetaConnection(String clientBusinessId, List<InstagramAccount> accounts, String accessToken) {}

    public static class ChannelException extends RuntimeException {
        public ChannelException(String message) {
            super(message);
        }

        public ChannelException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class WebhookSubscriptionException extends ChannelException {
        public WebhookSubscriptionException(String message) {
            super(message);
        }

        public WebhookSubscriptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
