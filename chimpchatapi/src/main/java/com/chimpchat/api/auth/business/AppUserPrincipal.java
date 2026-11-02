package com.chimpchat.api.auth.business;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.io.Serial;
import java.io.Serializable;

import com.chimpchat.api.auth.entity.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

public final class AppUserPrincipal implements UserDetails, OAuth2User, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final UUID id;
    private final String email;

    public AppUserPrincipal(UserEntity user) {
        id = user.getId();
        email = user.getEmail();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of("sub", id.toString(), "email", email);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getName() {
        return id.toString();
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }
}
