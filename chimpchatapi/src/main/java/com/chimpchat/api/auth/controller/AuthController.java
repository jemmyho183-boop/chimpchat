package com.chimpchat.api.auth.controller;

import com.chimpchat.api.auth.business.AppUserPrincipal;
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
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final EmailLinkBusiness emailLinkBusiness;
    private final LoginSessionBusiness loginSessionBusiness;
    private final boolean googleEnabled;

    public AuthController(
            EmailLinkBusiness emailLinkBusiness,
            LoginSessionBusiness loginSessionBusiness,
            ClientRegistrationRepository clientRegistrationRepository) {
        this.emailLinkBusiness = emailLinkBusiness;
        this.loginSessionBusiness = loginSessionBusiness;
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


    @PostMapping("/email-links")
    public EmailLinkResponseDto requestEmailLink(
            @Valid @RequestBody EmailLinkRequestDto request,
            HttpServletRequest servletRequest) {
        emailLinkBusiness.requestLink(request.email(), servletRequest.getRemoteAddr());
        return new EmailLinkResponseDto(
                "If sign-in by email is available, a one-time link will arrive shortly.");
    }

    @PostMapping("/email-links/consume")
    public CurrentUserResponseDto consumeEmailLink(
            @Valid @RequestBody ConsumeEmailLinkRequestDto request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        UserEntity user = emailLinkBusiness.consume(request.token());
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
    public void logout(HttpSession session) {
        session.invalidate();
        SecurityContextHolder.clearContext();
    }
}
