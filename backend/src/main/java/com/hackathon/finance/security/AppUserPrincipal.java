package com.hackathon.finance.security;

import com.hackathon.finance.entity.UserEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AppUserPrincipal(UUID id, String email, String password, boolean active) implements UserDetails {

    public static AppUserPrincipal from(UserEntity user) {
        return new AppUserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), user.isActive());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
