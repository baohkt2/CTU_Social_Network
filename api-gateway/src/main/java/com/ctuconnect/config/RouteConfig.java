package com.ctuconnect.config;

import com.ctuconnect.filter.JwtAuthenticationFilter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RedisRateLimiter authPublicRedisRateLimiter;
    private final KeyResolver ipKeyResolver;

    public RouteConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RedisRateLimiter authPublicRedisRateLimiter,
            KeyResolver ipKeyResolver) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authPublicRedisRateLimiter = authPublicRedisRateLimiter;
        this.ipKeyResolver = ipKeyResolver;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Public auth endpoints: rate limit by client IP (brute-force / abuse mitigation)
                .route("auth-public-rate-limit", r -> r
                        .path(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/refresh-token",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/auth/verify-email",
                                "/api/auth/resend-verification")
                        .filters(f -> f
                                .requestRateLimiter(rl -> rl
                                        .setRateLimiter(authPublicRedisRateLimiter)
                                        .setKeyResolver(ipKeyResolver))
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://auth-service"))
                // Auth Service Routes - Apply JWT filter (will skip public endpoints internally)
                .route("auth-service-route", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://auth-service"))

                // User Service Routes - Protected endpoints that require JWT validation
                .route("user-service-route", r -> r
                        .path("/api/users/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://user-service"))

                .route("media-service-route", r -> r
                        .path("/api/media/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://media-service"))

                .route("post-service-route", r -> r
                        .path("/api/posts/**", "/api/comments/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://post-service"))

                .route("notification-service-route", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://notification-service"))

                .route("chat-service-route", r -> r
                        .path("/api/chats/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://chat-service"))

                // WebSocket Chat Route - Special handling for WebSocket connections
                .route("chat-websocket-route", r -> r
                        .path("/api/ws/chat/**")
                        .filters(f -> f
                                .rewritePath("/api/ws/chat/(?<segment>.*)", "/ws/chat/${segment}")
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://chat-service"))

                // Recommendation Service Routes - AI-powered personalized recommendations
                // Route 1: /api/recommend/** (e.g., /api/recommend/posts, /api/recommend/feedback)
                .route("recommendation-api-route", r -> r
                        .path("/api/recommend/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://recommendation-service"))

                // Route 2: /api/recommendation/** (e.g., /api/recommendation/feed, /api/recommendation/interaction)
                .route("recommendation-feed-route", r -> r
                        .path("/api/recommendation/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://recommendation-service"))

                // Legacy route for /api/recommendations/** (post-service endpoints)
                // Keep this for backward compatibility if post-service has recommendation endpoints
                .route("post-recommendations-route", r -> r
                        .path("/api/recommendations/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://post-service"))

                // Feed route - can be handled by either service
                .route("feed-route", r -> r
                        .path("/api/feed/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://recommendation-service"))
                .build();
    }
}
