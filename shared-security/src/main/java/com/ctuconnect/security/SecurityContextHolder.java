package com.ctuconnect.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Security context to hold authenticated user information throughout the request lifecycle
 */
public class SecurityContextHolder {
    private static final Logger logger = LoggerFactory.getLogger(SecurityContextHolder.class);
    private static final ThreadLocal<AuthenticatedUser> context = new ThreadLocal<>();

    public static void setAuthenticatedUser(AuthenticatedUser user) {
        context.set(user);
        logger.debug("Set authenticated user: {}", user != null ? user.getUserId() : "null");
    }

    public static AuthenticatedUser getAuthenticatedUser() {
        return context.get();
    }

    public static void clear() {
        context.remove();
        logger.debug("Cleared security context");
    }

    public static String getCurrentUserId() {
        AuthenticatedUser user = getAuthenticatedUser();
        String userId = user != null ? user.getUserId() : null;
        if (userId == null) {
            logger.warn("getCurrentUserId() returned null - no authenticated user in context");
        }
        return userId;
    }

    public static String getCurrentUserIdOrThrow() {
        String userId = getCurrentUserId();
        if (userId == null) {
            throw new IllegalStateException("No authenticated user found in security context. Make sure API Gateway is sending X-User-Id header.");
        }
        return userId;
    }

    public static String getCurrentUserRole() {
        AuthenticatedUser user = getAuthenticatedUser();
        return user != null ? user.getRole() : null;
    }

    public static boolean isCurrentUserAdmin() {
        AuthenticatedUser user = getAuthenticatedUser();
        return user != null && user.isAdmin();
    }

    public static boolean hasAuthenticatedUser() {
        return getAuthenticatedUser() != null;
    }
}
