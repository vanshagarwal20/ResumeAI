package com.resumeai.auth.security;

import com.resumeai.auth.entity.User;
import com.resumeai.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * CustomUserDetailsService — Implements Spring Security's UserDetailsService.
 *
 * Spring Security calls loadUserByUsername() during authentication to get the user.
 * We use the email address as the "username" (Spring Security's terminology).
 *
 * This bridges our User entity with Spring Security's UserDetails interface,
 * enabling BCrypt password verification and role-based access control.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load user by their email address (Spring Security uses "username" as a generic term).
     *
     * Called automatically by Spring Security's DaoAuthenticationProvider.
     * After this returns, Spring compares the provided password against
     * the stored hash using the configured PasswordEncoder (BCrypt).
     *
     * @param email The email address submitted in the login form
     * @return UserDetails object for Spring Security to use
     * @throws UsernameNotFoundException if no user with this email exists
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                    new UsernameNotFoundException("User not found with email: " + email));

        // Map our User entity to Spring Security's UserDetails
        return buildUserDetails(user);
    }

    /**
     * Build a Spring Security UserDetails from our User entity.
     *
     * @param user Our domain User entity
     * @return UserDetails that Spring Security understands
     */
    private UserDetails buildUserDetails(User user) {
        // Convert our Role enum to Spring Security's GrantedAuthority format
        // Spring Security requires the "ROLE_" prefix for hasRole() checks
        Collection<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        // Spring Security's built-in User implementation
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash() != null ? user.getPasswordHash() : "")
                .authorities(authorities)
                .accountLocked(!user.getIsActive())  // Suspended users are "locked"
                .disabled(!user.getIsActive())        // Alternative to locked
                .build();
    }
}

