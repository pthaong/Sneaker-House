
package com.sneakerhouse.backend.security;

import com.sneakerhouse.backend.entity.UserAccount;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails, Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String email;
    private final String password;
    private final List<GrantedAuthority> authorities;

    public CustomUserDetails(UserAccount user) {
        this.id = user.getId();
        this.email = user.getEmail();

        this.password = user.getPasswordHash() == null
                ? "{noop}GOOGLE_ONLY_ACCOUNT"
                : user.getPasswordHash();

        String role = user.getRole();

        if (role == null || role.isBlank()) {
            role = "CUSTOMER";
        }

        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }

        this.authorities = List.of(
                new SimpleGrantedAuthority(role)
        );
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
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
}
