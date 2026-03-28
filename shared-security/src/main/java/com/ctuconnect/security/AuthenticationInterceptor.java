package com.ctuconnect.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to extract authenticated user information from request headers
 * These headers are set by the API Gateway after JWT token validation
 */
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationInterceptor.class);

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Extract user information from headers set by gateway
        String userId = request.getHeader(USER_ID_HEADER);
        String email = request.getHeader(USER_EMAIL_HEADER);
        String role = request.getHeader(USER_ROLE_HEADER);

        logger.debug("Authentication headers - UserId: {}, Email: {}, Role: {}", userId, email, role);

        // If user information is present, create authenticated user context
        if (userId != null && !userId.trim().isEmpty()) {
            // Set default values if email or role is missing
            if (email == null || email.trim().isEmpty()) {
                email = "unknown@unknown.com";
                logger.warn("Missing X-User-Email header, using default value");
            }
            if (role == null || role.trim().isEmpty()) {
                role = "USER";
                logger.warn("Missing X-User-Role header, using default role: USER");
            }

            AuthenticatedUser authenticatedUser = new AuthenticatedUser(userId.trim(), email.trim(), role.trim());
            SecurityContextHolder.setAuthenticatedUser(authenticatedUser);
            logger.debug("Set authenticated user in SecurityContext: {}", userId);
        } else {
            logger.warn("Missing or empty X-User-Id header. User authentication context not set.");
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Clear security context after request completion
        SecurityContextHolder.clear();
        logger.debug("Cleared SecurityContext after request completion");
    }
}
