package com.chimpchat.api.auth.business;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import com.chimpchat.api.auth.entity.RefreshTokenEntity;
import com.chimpchat.api.auth.entity.UserEntity;
import com.chimpchat.api.auth.repository.RefreshTokenRepository;
import com.chimpchat.api.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthTokenBusiness {
    public static final String ACCESS_COOKIE = "chimpchat_access";
    public static final String REFRESH_COOKIE = "chimpchat_refresh";
    private static final String TOKEN_TYPE = "token_type";
    private static final String ACCESS = "access";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final byte[] signingSecret;
    private final Duration accessLifetime;
    private final Duration refreshLifetime;
    private final boolean secureCookies;

    public AuthTokenBusiness(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            @Value("${app.security.jwt.secret}") String signingSecret,
            @Value("${app.security.jwt.access-ttl:PT1H}") Duration accessLifetime,
            @Value("${app.security.jwt.refresh-ttl:P15D}") Duration refreshLifetime,
            @Value("${app.security.jwt.cookie-secure:true}") boolean secureCookies) {
        if (signingSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("app.security.jwt.secret must be at least 32 bytes.");
        }
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.signingSecret = signingSecret.getBytes(StandardCharsets.UTF_8);
        this.accessLifetime = accessLifetime;
        this.refreshLifetime = refreshLifetime;
        this.secureCookies = secureCookies;
    }

    @Transactional
    public void issue(UserEntity user, HttpServletResponse response) {
        Instant now = Instant.now();
        String accessToken = Jwts.builder()
                .subject(user.getId().toString())
                .claim(TOKEN_TYPE, ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessLifetime)))
                .signWith(Keys.hmacShaKeyFor(signingSecret))
                .compact();

        byte[] random = new byte[64];
        RANDOM.nextBytes(random);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        refreshTokenRepository.save(new RefreshTokenEntity(
                user, hash(refreshToken), now.plus(refreshLifetime)));

        addCookie(response, ACCESS_COOKIE, accessToken, (int) accessLifetime.toSeconds());
        addCookie(response, REFRESH_COOKIE, refreshToken, (int) refreshLifetime.toSeconds());
    }

    @Transactional
    public UserEntity refresh(String rawRefreshToken, HttpServletResponse response) {
        RefreshTokenEntity stored = refreshTokenRepository
                .findActiveForUpdate(hash(rawRefreshToken), Instant.now())
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "refresh_token_expired",
                        "Your session has expired. Please sign in again."));
        stored.revoke(Instant.now());
        UserEntity user = stored.getUser();
        issue(user, response);
        return user;
    }

    public UserEntity authenticateAccess(String rawAccessToken) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(signingSecret))
                    .build()
                    .parseSignedClaims(rawAccessToken)
                    .getPayload();
            if (!ACCESS.equals(claims.get(TOKEN_TYPE, String.class))) {
                throw invalidAccessToken();
            }
            return userRepository.findById(java.util.UUID.fromString(claims.getSubject()))
                    .orElseThrow(this::invalidAccessToken);
        } catch (ExpiredJwtException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalidAccessToken();
        }
    }

    public void clear(HttpServletResponse response) {
        addCookie(response, ACCESS_COOKIE, "", 0);
        addCookie(response, REFRESH_COOKIE, "", 0);
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findActiveForUpdate(hash(rawRefreshToken), Instant.now())
                .ifPresent(token -> token.revoke(Instant.now()));
    }

    private AuthException invalidAccessToken() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "invalid_access_token",
                "Your access token is invalid. Please sign in again.");
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookies);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private String hash(String rawToken) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the runtime.", exception);
        }
    }
}
