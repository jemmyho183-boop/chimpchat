package com.chimpchat.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.chimpchat.api.OAuthService.UnifiedLoginConnection;
import com.chimpchat.api.OAuthService.UnifiedLoginException;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/auth/instagram")
public class OAuthController {
    private static final Logger logger = LoggerFactory.getLogger(OAuthController.class);
    private static final String STATE_SESSION_KEY = "instagram_oauth_state";
    private static final String CONNECTION_SESSION_KEY = "instagram_connection";

    private final OAuthService oauthService;
    private final String frontendUrl;

    public OAuthController(OAuthService oauthService, @Value("${app.frontend-url}") String frontendUrl) {
        this.oauthService = oauthService;
        this.frontendUrl = frontendUrl;
    }

    @GetMapping
    public ResponseEntity<Void> start(HttpSession session) {
        String state = UUID.randomUUID().toString();
        try {
            String authorizationUrl = oauthService.authorizationUrl(state);
            session.setAttribute(STATE_SESSION_KEY, state);
            return ResponseEntity.status(302).location(URI.create(authorizationUrl)).build();
        } catch (UnifiedLoginException exception) {
            return redirect("oauth_error", exception.getMessage());
        }
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpSession session) {
        Object expectedState = session.getAttribute(STATE_SESSION_KEY);
        session.removeAttribute(STATE_SESSION_KEY);
        if (expectedState == null || state == null || !expectedState.equals(state)) {
            return redirect("oauth_error", "Instagram login expired. Please try again.");
        }
        if (error != null) {
            return redirect("oauth_error", "Instagram login was cancelled or denied.");
        }
        if (code == null || code.isBlank()) {
            return redirect("oauth_error", "Instagram did not return an authorization code.");
        }

        try {
            UnifiedLoginConnection connection = oauthService.exchangeCode(code);
            session.setAttribute(CONNECTION_SESSION_KEY, connection);
            String usernames = connection.accounts().stream()
                    .map(account -> "@" + account.username())
                    .reduce((first, second) -> first + ", " + second)
                    .orElse("");
            return redirect("instagram_username", usernames);
        } catch (UnifiedLoginException exception) {
            logger.warn("Instagram login failed: {}", exception.getMessage());
            return redirect("oauth_error", "Instagram login could not be completed. Check the app credentials and redirect URI.");
        }
    }

    @GetMapping("/account")
    public ResponseEntity<?> account(HttpSession session) {
        Object connection = session.getAttribute(CONNECTION_SESSION_KEY);
        if (!(connection instanceof UnifiedLoginConnection unifiedConnection)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(new AccountResponse(
                unifiedConnection.clientBusinessId(),
                unifiedConnection.accounts().stream()
                        .map(account -> new ConnectedInstagramAccount(account.id(), account.username()))
                        .toList()));
    }

    private ResponseEntity<Void> redirect(String key, String value) {
        String url = UriComponentsBuilder.fromUriString(frontendUrl)
                .queryParam(key, value)
                .build()
                .encode()
                .toUriString();
        return ResponseEntity.status(302).header(HttpHeaders.LOCATION, url).build();
    }

    record AccountResponse(String clientBusinessId, List<ConnectedInstagramAccount> accounts) {}

    record ConnectedInstagramAccount(String id, String username) {}
}
