package com.chimpchat.api.auth.business;

import java.io.IOException;

import com.chimpchat.api.auth.entity.UserEntity;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GoogleAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthenticationSuccessHandler.class);

    private final GoogleIdentityBusiness identityBusiness;
    private final LoginSessionBusiness loginSessionBusiness;
    private final AuthTokenBusiness tokenBusiness;
    private final String frontendUrl;

    public GoogleAuthenticationSuccessHandler(
            GoogleIdentityBusiness identityBusiness,
            LoginSessionBusiness loginSessionBusiness,
            AuthTokenBusiness tokenBusiness,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.identityBusiness = identityBusiness;
        this.loginSessionBusiness = loginSessionBusiness;
        this.tokenBusiness = tokenBusiness;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OidcUser googleUser)) {
            response.sendRedirect(frontendRedirect("google_sign_in_failed"));
            return;
        }

        try {
            UserEntity user = identityBusiness.resolve(googleUser);
            loginSessionBusiness.establish(user, request, response);
            tokenBusiness.issue(user, response);
            response.sendRedirect(frontendRedirect(null));
        } catch (AuthException exception) {
            logger.info("Google sign-in rejected: {}", exception.getCode());
            response.sendRedirect(frontendRedirect(exception.getCode()));
        }
    }

    private String frontendRedirect(String authError) {
        UriComponentsBuilder redirect = UriComponentsBuilder.fromUriString(frontendUrl)
                .replacePath("/")
                .replaceQuery(null);
        if (authError != null) {
            redirect.queryParam("auth_error", authError);
        }
        return redirect.build().encode().toUriString();
    }
}
