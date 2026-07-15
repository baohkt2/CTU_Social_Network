package com.ctuconnect.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.http.HttpCookie;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Arrays;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Value("${jwt.secret:XpExu6h1RJoY1qFZyLVzJbor/aYutNR2AD86ZM/tKqc=}")
    private String secretKey;

    private final List<String> openApiEndpoints = Arrays.asList(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/refresh-token",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/verify-email",
            "/api/users/categories/**",
            "/api/test",
            "/actuator"
    );

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            System.out.println("JwtAuthenticationFilter: Processing request path: " + path);

            // Check if this is an open API endpoint FIRST before any token processing
            if (isOpenApiEndpoint(request)) {
                System.out.println("JwtAuthenticationFilter: Path '" + path + "' is open API endpoint, skipping authentication.");
                return chain.filter(exchange);
            }

            String accessToken = null;
            String refreshToken = null;

            // Extract tokens from cookies
            MultiValueMap<String, HttpCookie> cookies = request.getCookies();

            // Try new naming convention first (camelCase)
            HttpCookie accessTokenCookie = cookies.getFirst("accessToken");
            HttpCookie refreshTokenCookie = cookies.getFirst("refreshToken");

            // Fallback to old naming convention (snake_case) for backward compatibility
            if (accessTokenCookie == null) {
                accessTokenCookie = cookies.getFirst("access_token");
            }
            if (refreshTokenCookie == null) {
                refreshTokenCookie = cookies.getFirst("refresh_token");
            }

            if (accessTokenCookie != null) {
                accessToken = accessTokenCookie.getValue();
                System.out.println("JwtAuthenticationFilter: Access token extracted from cookie.");
                System.out.println("JwtAuthenticationFilter: DEBUG - Access token value: " + accessToken);
            }
            if (refreshTokenCookie != null && path.equals("/api/auth/refresh-token")) {
                refreshToken = refreshTokenCookie.getValue();
                System.out.println("JwtAuthenticationFilter: Refresh token extracted from cookie.");
            }

            // Fallback to Authorization header for access token
            if (accessToken == null && request.getHeaders().containsKey("Authorization")) {
                String authHeader = request.getHeaders().getFirst("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    accessToken = authHeader.substring(7);
                    System.out.println("JwtAuthenticationFilter: Access token extracted from 'Authorization' header.");
                }
            }
            
            // For WebSocket connections, try to get token from query parameter
            if (accessToken == null && path.startsWith("/api/ws/")) {
                String tokenParam = request.getQueryParams().getFirst("token");
                if (tokenParam != null && !tokenParam.isEmpty()) {
                    accessToken = tokenParam;
                    System.out.println("JwtAuthenticationFilter: Access token extracted from query parameter for WebSocket.");
                }
            }

            // Require refresh token for /api/auth/refresh-token, access token for others
            if (accessToken == null && refreshToken == null) {
                System.err.println("JwtAuthenticationFilter: No authentication token found for path: " + path);
                return onError(exchange, "Authentication token is missing", HttpStatus.UNAUTHORIZED);
            }

            try {
                String tokenToValidate = path.equals("/api/auth/refresh-token") ? refreshToken : accessToken;
                if (tokenToValidate == null) {
                    return onError(exchange, "Required token is missing", HttpStatus.UNAUTHORIZED);
                }

                Claims claims = extractAllClaims(tokenToValidate);

                // Check token expiration
                if (claims.getExpiration() != null && claims.getExpiration().before(new Date())) {
                    System.err.println("JwtAuthenticationFilter: Token has expired for user: " + claims.getSubject());
                    return onError(exchange, "Token has expired", HttpStatus.UNAUTHORIZED);
                }

                // Extract user information from JWT claims
                String userId = claims.get("userId", String.class); // Extract userId from claims
                String userEmail = claims.getSubject(); // Subject contains email
                String userRole = claims.get("role", String.class);

                System.out.println("JwtAuthenticationFilter: DEBUG - Extracted userEmail from claims: " + userEmail);
                System.out.println("JwtAuthenticationFilter: DEBUG - Extracted userId from claims: " + userId);
                System.out.println("JwtAuthenticationFilter: DEBUG - Extracted userRole from claims: " + userRole);

                // Fallback to subject if userId claim is not present (backward compatibility)
                if (userId == null || userId.trim().isEmpty()) {
                    userId = userEmail;
                }

                // Add headers with consistent naming for user-service
                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .header("X-User-Id", userId != null ? userId : "")
                        .header("X-User-Email", userEmail != null ? userEmail : "")
                        .header("X-User-Role", userRole != null ? userRole : "USER")
                        // Keep old headers for backward compatibility
                        .header("X-Auth-User-Id", userId != null ? userId : "")
                        .header("X-Auth-User-Role", userRole != null ? userRole : "USER")
                        .build();

                System.out.println("JwtAuthenticationFilter: DEBUG - Headers set - X-User-Id: " + userId + ", X-User-Email: " + userEmail + ", X-User-Role: " + userRole);
                System.out.println("JwtAuthenticationFilter: Token validated for user: " + userId + ", role: " + userRole + ", email: " + userEmail);
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            } catch (ExpiredJwtException e) {
                System.err.println("JwtAuthenticationFilter: JWT token is expired: " + e.getMessage());
                return onError(exchange, "JWT token is expired", HttpStatus.UNAUTHORIZED);
            } catch (MalformedJwtException e) {
                System.err.println("JwtAuthenticationFilter: Invalid JWT token format: " + e.getMessage());
                return onError(exchange, "Invalid JWT token format", HttpStatus.UNAUTHORIZED);
            } catch (UnsupportedJwtException e) {
                System.err.println("JwtAuthenticationFilter: JWT token is unsupported: " + e.getMessage());
                return onError(exchange, "Unsupported JWT token", HttpStatus.UNAUTHORIZED);
            } catch (IllegalArgumentException e) {
                System.err.println("JwtAuthenticationFilter: JWT claims string is empty or malformed: " + e.getMessage());
                return onError(exchange, "JWT claims string is empty or malformed", HttpStatus.UNAUTHORIZED);
            } catch (Exception e) {
                System.err.println("JwtAuthenticationFilter: Unexpected error during token validation: " + e.getMessage());
                return onError(exchange, "Unexpected error during authentication", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
    }

    private boolean isOpenApiEndpoint(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        for (String uri : openApiEndpoints) {
            if (path.startsWith(uri)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        String errorBody = "{\"error\": \"" + message + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(errorBody.getBytes(StandardCharsets.UTF_8));
        response.getHeaders().add("Content-Type", "application/json");
        return response.writeWith(Mono.just(buffer));
    }

    private Claims extractAllClaims(String token) {
        // Correct way for JJWT 0.12.x+
        return Jwts.parser() // Changed from parserBuilder() to parser()
                .verifyWith((SecretKey) getSigningKey()) // Changed from setSigningKey() to verifyWith()
                .build()
                .parseSignedClaims(token) // Correct for 0.12.x+
                .getPayload(); // Correct for 0.12.x+
    }

    private Key getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public static class Config {
    }
}