package com.sneakerhouse.backend.service;

import com.sneakerhouse.backend.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserAccountRepository users;
    public CustomUserDetailsService(UserAccountRepository users) { this.users = users; }

    @Override @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = users.findByEmailIgnoreCase(username)
            .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
        String hash = user.getPasswordHash() == null ? "{noop}GOOGLE_ONLY_ACCOUNT" : user.getPasswordHash();
        return User.withUsername(user.getEmail()).password(hash)
            .roles(user.getRole()).build();
    }
}
