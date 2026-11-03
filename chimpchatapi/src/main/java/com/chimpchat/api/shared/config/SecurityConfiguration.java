package com.chimpchat.api.shared.config;

import com.chimpchat.api.auth.business.GoogleAuthenticationSuccessHandler;
import com.chimpchat.api.auth.business.AuthTokenFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain applicationSecurity(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            GoogleAuthenticationSuccessHandler googleSuccessHandler,
            AuthTokenFilter authTokenFilter) throws Exception {
        http
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/health",
                                "/api/v1/auth/csrf",
                                "/api/v1/auth/options",
                                "/api/v1/auth/email-links",
                                "/api/v1/auth/email-links/consume",
                                "/api/v1/auth/refresh",
                                "/actuator/health",
                                "/actuator/info",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/error")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth -> oauth
                        .successHandler(googleSuccessHandler)
                        .failureHandler((request, response, exception) ->
                                response.sendRedirect("/?auth_error=google_sign_in_failed")))
                .logout(logout -> logout.disable())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .sessionManagement(session -> session.sessionFixation(fixation -> fixation.migrateSession()));
        http.addFilterBefore(authTokenFilter,
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
