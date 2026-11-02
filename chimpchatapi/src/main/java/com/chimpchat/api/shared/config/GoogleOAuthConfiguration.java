package com.chimpchat.api.shared.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@Configuration
public class GoogleOAuthConfiguration {
    @Bean
    ClientRegistrationRepository clientRegistrationRepository(
            @Value("${app.auth.google.client-id:}") String clientId,
            @Value("${app.auth.google.client-secret:}") String clientSecret,
            @Value("${app.auth.google.redirect-uri:http://localhost:3000/login/oauth2/code/google}") String redirectUri,
            @Value("${app.auth.google.enabled:true}") boolean enabled) {
        if (!enabled || clientId.isBlank() || clientSecret.isBlank()) {
            return registrationId -> null;
        }

        ClientRegistration google = ClientRegistration.withRegistrationId("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirectUri)
                .scope(List.of("openid", "email", "profile"))
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();
        return new InMemoryClientRegistrationRepository(google);
    }
}
