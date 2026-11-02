package com.chimpchat.api.channel.controller;

import java.net.URI;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.chimpchat.api.auth.business.AppUserPrincipal;
import com.chimpchat.api.channel.business.InstagramChannelBusiness;
import com.chimpchat.api.channel.business.MetaInstagramBusiness;
import com.chimpchat.api.channel.business.MetaInstagramBusiness.MetaConnection;
import com.chimpchat.api.channel.business.TokenEncryptionBusiness;
import com.chimpchat.api.channel.dto.ChannelStatusResponseDto;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/channels")
public class ChannelController {
    private static final Logger logger = LoggerFactory.getLogger(ChannelController.class);
    private static final String STATE_SESSION_KEY = "instagram_oauth_states";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final MetaInstagramBusiness metaInstagramBusiness;
    private final InstagramChannelBusiness instagramChannelBusiness;
    private final TokenEncryptionBusiness tokenEncryptionBusiness;
    private final String frontendUrl;

    public ChannelController(
            MetaInstagramBusiness metaInstagramBusiness,
            InstagramChannelBusiness instagramChannelBusiness,
            TokenEncryptionBusiness tokenEncryptionBusiness,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.metaInstagramBusiness = metaInstagramBusiness;
        this.instagramChannelBusiness = instagramChannelBusiness;
        this.tokenEncryptionBusiness = tokenEncryptionBusiness;
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/instagram")
    public ChannelStatusResponseDto instagramStatus(@AuthenticationPrincipal AppUserPrincipal principal) {
        return instagramChannelBusiness.status(principal.getId());
    }

    @GetMapping("/instagram/oauth/start")
    public ResponseEntity<Void> startInstagramOAuth(
            @AuthenticationPrincipal AppUserPrincipal principal,
            HttpSession session) {
            if (!metaInstagramBusiness.isConfigured()) {
                return frontendRedirect("channel_error", "instagram_not_configured");
            }
            if (!tokenEncryptionBusiness.isConfigured()) {
                return frontendRedirect("channel_error", "token_encryption_unavailable");
            }
            try {
            String state = newState();
            Map<String, String> states = currentStates(session);
            if (states.size() >= 5) {
                states.remove(states.keySet().iterator().next());
            }
            states.put(state, principal.getId() + ":" + System.currentTimeMillis());
            session.setAttribute(STATE_SESSION_KEY, states);
            return ResponseEntity.status(302)
                    .location(URI.create(metaInstagramBusiness.authorizationUrl(state)))
                    .build();
        } catch (MetaInstagramBusiness.ChannelException exception) {
            logger.warn("Instagram OAuth could not start: {}", exception.getMessage());
            return frontendRedirect("channel_error", "instagram_not_configured");
        }
    }

    @GetMapping("/instagram/oauth/callback")
    public ResponseEntity<Void> instagramCallback(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpSession session) {
        String expected = consumeState(session, state);
        if (expected == null || state == null || !expected.startsWith(principal.getId() + ":")
                || !isFresh(expected)) {
            return frontendRedirect("channel_error", "instagram_login_expired");
        }
        if (error != null) {
            return frontendRedirect("channel_error", "instagram_login_cancelled");
        }
        if (code == null || code.isBlank()) {
            return frontendRedirect("channel_error", "instagram_code_missing");
        }

        try {
            MetaConnection connection = metaInstagramBusiness.exchangeCode(code);
            instagramChannelBusiness.connect(principal.getId(), connection);
            return frontendRedirect("channel_connected", "instagram");
        } catch (MetaInstagramBusiness.ChannelException exception) {
            logger.warn("Instagram connection failed: {}", exception.getMessage());
            return frontendRedirect("channel_error", "instagram_connection_failed");
        } catch (InstagramChannelBusiness.ChannelException exception) {
            logger.warn("Instagram connection could not be stored: {}", exception.getCode());
            return frontendRedirect("channel_error", exception.getCode());
        } catch (TokenEncryptionBusiness.ChannelException exception) {
            logger.warn("Instagram token could not be stored: {}", exception.getCode());
            return frontendRedirect("channel_error", exception.getCode());
        }
    }

    @DeleteMapping("/{connectionId}")
    public ResponseEntity<Void> disconnect(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID connectionId) {
        instagramChannelBusiness.disconnect(principal.getId(), connectionId);
        return ResponseEntity.noContent().build();
    }

    private String newState() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Map<String, String> currentStates(HttpSession session) {
        Object existing = session.getAttribute(STATE_SESSION_KEY);
        if (existing instanceof Map<?, ?> map) {
            Map<String, String> states = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String key && entry.getValue() instanceof String value) {
                    states.put(key, value);
                }
            }
            return states;
        }
        return new HashMap<>();
    }

    private String consumeState(HttpSession session, String state) {
        if (state == null) {
            return null;
        }
        Map<String, String> states = currentStates(session);
        String expected = states.remove(state);
        session.setAttribute(STATE_SESSION_KEY, states);
        return expected;
    }

    private boolean isFresh(String stateValue) {
        int separator = stateValue.lastIndexOf(':');
        if (separator < 0) {
            return false;
        }
        try {
            long issuedAt = Long.parseLong(stateValue.substring(separator + 1));
            return System.currentTimeMillis() - issuedAt < 10 * 60 * 1000L;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private ResponseEntity<Void> frontendRedirect(String key, String value) {
        String url = UriComponentsBuilder.fromUriString(frontendUrl)
                .queryParam(key, value)
                .build()
                .encode()
                .toUriString();
        return ResponseEntity.status(302).header(HttpHeaders.LOCATION, url).build();
    }
}
