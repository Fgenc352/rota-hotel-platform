package com.hotelbooking.identity.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StaffUserDetailsService implements UserDetailsService {
    private final StaffRepository staff;

    @Override
    public UserDetails loadUserByUsername(String username) {
        var credentials = staff.credentials(username).orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        return User.withUsername(credentials.username()).password(credentials.passwordHash()).roles("STAFF").build();
    }
}
