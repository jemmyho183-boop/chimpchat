package com.chimpchat.api;

import java.net.URI;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class OAuthController {
    private final OAuthService oauthService;
    private final String frontendUrl;

    public OAuthController(OAuthService oauthService, @Value("${app.frontend-url}") String frontendUrl) {
        this.oauthService = oauthService;
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/{provider}")
    public ResponseEntity<Void> start(@PathVariable String provider, HttpServletResponse response) {
        String state = UUID.randomUUID().toString();
        ResponseCookie cookie = ResponseCookie.from("oauth_state", state).httpOnly(true).secure(false)
                .sameSite("Lax").path("/api/auth").maxAge(600).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.status(302).location(URI.create(oauthService.authorizationUrl(provider, state))).build();
    }

    @GetMapping("/{provider}/callback")
    public ResponseEntity<Void> callback(@PathVariable String provider, @RequestParam String code,
            @RequestParam String state, @org.springframework.web.bind.annotation.CookieValue(name = "oauth_state") String savedState,
            HttpServletResponse response) {
        if (!state.equals(savedState)) return redirect("oauth_error", "Invalid OAuth state");
        try {
            oauthService.exchange(provider, code);
            response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("oauth_state", "").path("/api/auth").maxAge(0).build().toString());
            return redirect("auth", "success");
        } catch (Exception ex) {
            return redirect("oauth_error", "Unable to connect your account");
        }
    }

    private ResponseEntity<Void> redirect(String key, String value) {
        String url = UriComponentsBuilder.fromUriString(frontendUrl).queryParam(key, value).build().toUriString();
        return ResponseEntity.status(302).location(URI.create(url)).build();
    }
}
