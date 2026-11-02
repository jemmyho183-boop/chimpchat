package com.chimpchat.api.auth.business;

import java.io.IOException;

import com.chimpchat.api.auth.entity.UserEntity;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class GoogleAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthenticationSuccessHandler.class);

    private final GoogleIdentityBusiness identityBusiness;
    private final LoginSessionBusiness loginSessionBusiness;

    public GoogleAuthenticationSuccessHandler(
            GoogleIdentityBusiness identityBusiness,
            LoginSessionBusiness loginSessionBusiness) {
        this.identityBusiness = identityBusiness;
        this.loginSessionBusiness = loginSessionBusiness;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OidcUser googleUser)) {
            response.sendRedirect("/?auth_error=google_sign_in_failed");
            return;
        }

        try {
            UserEntity user = identityBusiness.resolve(googleUser);
            loginSessionBusiness.establish(user, request, response);
            response.sendRedirect("/");
        } catch (AuthException exception) {
            logger.info("Google sign-in rejected: {}", exception.getCode());
            response.sendRedirect("/?auth_error=" + exception.getCode());
        }
    }
}
