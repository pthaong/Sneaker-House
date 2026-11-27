
package com.sneakerhouse.backend.service;

import com.sneakerhouse.backend.entity.UserAccount;
import com.sneakerhouse.backend.repository.UserAccountRepository;
import com.sneakerhouse.backend.security.CustomUserDetails;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository users;

    public CustomUserDetailsService(UserAccountRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        UserAccount user = users.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Invalid email or password"
                ));

        return new CustomUserDetails(user);
    }
}
