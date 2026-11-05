package com.chimpchat.api.auth.controller;

import com.chimpchat.api.auth.business.AppUserPrincipal;
import com.chimpchat.api.auth.business.AuthException;
import com.chimpchat.api.auth.business.AuthTokenBusiness;
import com.chimpchat.api.auth.business.EmailLinkBusiness;
import com.chimpchat.api.auth.business.LoginSessionBusiness;
import com.chimpchat.api.auth.dto.AuthOptionsResponseDto;
import com.chimpchat.api.auth.dto.ConsumeEmailLinkRequestDto;
import com.chimpchat.api.auth.dto.CsrfResponseDto;
import com.chimpchat.api.auth.dto.CurrentUserResponseDto;
import com.chimpchat.api.auth.dto.EmailLinkRequestDto;
import com.chimpchat.api.auth.dto.EmailLinkResponseDto;
import com.chimpchat.api.auth.entity.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping({ "/api/auth", "/api/v1/auth" })
public class AuthController {
    private final EmailLinkBusiness emailLinkBusiness;
    private final LoginSessionBusiness loginSessionBusiness;
    private final AuthTokenBusiness tokenBusiness;
    private final boolean googleEnabled;

    public AuthController(
            EmailLinkBusiness emailLinkBusiness,
            LoginSessionBusiness loginSessionBusiness,
            AuthTokenBusiness tokenBusiness,
            ClientRegistrationRepository clientRegistrationRepository) {
        this.emailLinkBusiness = emailLinkBusiness;
        this.loginSessionBusiness = loginSessionBusiness;
        this.tokenBusiness = tokenBusiness;
        this.googleEnabled = clientRegistrationRepository.findByRegistrationId("google") != null;
    }

    @GetMapping("/csrf")
    public CsrfResponseDto csrf(CsrfToken csrfToken) {
        return new CsrfResponseDto(csrfToken.getToken());
    }

    @GetMapping("/options")
    public AuthOptionsResponseDto options() {
        return new AuthOptionsResponseDto(googleEnabled, emailLinkBusiness.isEnabled());
    }

    @GetMapping("/google/start")
    public void startGoogleAuthentication(HttpServletResponse response) throws java.io.IOException {
        if (!googleEnabled) {
            response.sendRedirect("/?auth_error=google_sign_in_unavailable");
            return;
        }
        response.sendRedirect("/oauth2/authorization/google");
    }

    @PostMapping({ "/magic-link/request", "/email-links" })
    public EmailLinkResponseDto requestEmailLink(
            @Valid @RequestBody EmailLinkRequestDto request,
            HttpServletRequest servletRequest) {
        emailLinkBusiness.requestLink(request.email(), servletRequest.getRemoteAddr());
        return new EmailLinkResponseDto(
                "If sign-in by email is available, a one-time link will arrive shortly.");
    }

    @PostMapping({ "/magic-link/verify", "/email-links/consume" })
    public CurrentUserResponseDto consumeEmailLink(
            @Valid @RequestBody ConsumeEmailLinkRequestDto request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        UserEntity user = emailLinkBusiness.consume(request.token());
        loginSessionBusiness.establish(user, servletRequest, servletResponse);
        tokenBusiness.issue(user, servletResponse);
        return new CurrentUserResponseDto(user.getId(), user.getEmail());
    }

    @PostMapping("/refresh")
    public CurrentUserResponseDto refresh(
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        String rawRefreshToken = cookie(servletRequest, AuthTokenBusiness.REFRESH_COOKIE);
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "refresh_token_expired",
                    "Your session has expired. Please sign in again.");
        }
        UserEntity user = tokenBusiness.refresh(rawRefreshToken, servletResponse);
        loginSessionBusiness.establish(user, servletRequest, servletResponse);
        return new CurrentUserResponseDto(user.getId(), user.getEmail());
    }

    @GetMapping("/me")
    public CurrentUserResponseDto currentUser(@AuthenticationPrincipal AppUserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return new CurrentUserResponseDto(principal.getId(), principal.getEmail());
    }

    @PostMapping("/logout")
    public void logout(
            HttpServletRequest request,
            HttpSession session,
            HttpServletResponse response) {
        tokenBusiness.revoke(cookie(request, AuthTokenBusiness.REFRESH_COOKIE));
        session.invalidate();
        SecurityContextHolder.clearContext();
        tokenBusiness.clear(response);
    }

    private String cookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
