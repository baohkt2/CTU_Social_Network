package com.ctuconnect.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the authenticated user information passed from the gateway
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticatedUser {
    private String userId;
    private String email;
    private String role;

    public boolean hasRole(String role) {
        return this.role != null && this.role.equalsIgnoreCase(role);
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public boolean isUser() {
        return hasRole("USER");
    }

    public boolean isAuthenticated() {
        return userId != null && !userId.isEmpty() &&
               email != null && !email.isEmpty() &&
               role != null && !role.isEmpty();
    }

    public String getId() {
        return userId;
    }
}
