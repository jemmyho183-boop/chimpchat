package com.chimpchat.api.auth.business;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {
    private final AuthTokenBusiness tokenBusiness;

    public AuthTokenFilter(AuthTokenBusiness tokenBusiness) {
        this.tokenBusiness = tokenBusiness;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String access = cookie(request, AuthTokenBusiness.ACCESS_COOKIE);
        if (access != null) {
            try {
                authenticate(request, tokenBusiness.authenticateAccess(access));
            } catch (io.jsonwebtoken.ExpiredJwtException expired) {
                String refresh = cookie(request, AuthTokenBusiness.REFRESH_COOKIE);
                if (refresh != null) {
                    try {
                        authenticate(request, tokenBusiness.refresh(refresh, response));
                    } catch (AuthException ignored) {
                        SecurityContextHolder.clearContext();
                    }
                } else {
                    SecurityContextHolder.clearContext();
                }
            } catch (AuthException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, com.chimpchat.api.auth.entity.UserEntity user) {
        AppUserPrincipal principal = new AppUserPrincipal(user);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String cookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
