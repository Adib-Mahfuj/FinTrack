package com.adib.fintrack.security;

import org.springframework.security.core.userdetails.User;

import java.time.LocalDateTime;
import java.util.Collections;

// Spring's User plus the moment the password last changed, so the JWT filter can reject older tokens
public class AppUserDetails extends User {

    private final LocalDateTime passwordChangedAt;

    public AppUserDetails(String username, String password, LocalDateTime passwordChangedAt) {
        super(username, password, Collections.emptyList());
        this.passwordChangedAt = passwordChangedAt;
    }

    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }
}