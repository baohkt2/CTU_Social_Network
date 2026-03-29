package com.ctuconnect.config;

import com.ctuconnect.filter.JwtAuthenticationFilter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
public class RouteConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RedisRateLimiter publicRateLimiter;
    private final RedisRateLimiter authenticatedRateLimiter;
    private final KeyResolver ipKeyResolver;

    public RouteConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                       @org.springframework.beans.factory.annotation.Qualifier("publicRateLimiter") RedisRateLimiter publicRateLimiter,
                       @org.springframework.beans.factory.annotation.Qualifier("authenticatedRateLimiter") RedisRateLimiter authenticatedRateLimiter,
                       KeyResolver ipKeyResolver) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.publicRateLimiter = publicRateLimiter;
        this.authenticatedRateLimiter = authenticatedRateLimiter;
        this.ipKeyResolver = ipKeyResolver;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // Auth Service — tighter limit for login/register to slow brute-force
                .route("auth-service-route", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(publicRateLimiter);
                                    c.setKeyResolver(ipKeyResolver);
                                    c.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                                }))
                        .uri("lb://auth-service"))

                // User Service
                .route("user-service-route", r -> r
                        .path("/api/users/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(authenticatedRateLimiter);
                                    c.setKeyResolver(ipKeyResolver);
                                    c.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                                }))
                        .uri("lb://user-service"))

                // Media Service
                .route("media-service-route", r -> r
                        .path("/api/media/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(authenticatedRateLimiter);
                                    c.setKeyResolver(ipKeyResolver);
                                    c.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                                }))
                        .uri("lb://media-service"))

                // Post Service
                .route("post-service-route", r -> r
                        .path("/api/posts/**", "/api/comments/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(authenticatedRateLimiter);
                                    c.setKeyResolver(ipKeyResolver);
                                    c.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                                }))
                        .uri("lb://post-service"))

                // Notification Service
                .route("notification-service-route", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(authenticatedRateLimiter);
                                    c.setKeyResolver(ipKeyResolver);
                                    c.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                                }))
                        .uri("lb://notification-service"))

                // Chat Service (HTTP)
                .route("chat-service-route", r -> r
                        .path("/api/chats/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(authenticatedRateLimiter);
                                    c.setKeyResolver(ipKeyResolver);
                                    c.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                                }))
                        .uri("lb://chat-service"))

                // Chat Service (WebSocket) — rate limit applies to handshake only
                .route("chat-websocket-route", r -> r
                        .path("/api/ws/chat/**")
                        .filters(f -> f
                                .rewritePath("/api/ws/chat/(?<segment>.*)", "/ws/chat/${segment}")
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri("lb://chat-service"))

                // Recommendation Service
                .route("recommendation-api-route", r -> r
                        .path("/api/recommend/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(authenticatedRateLimiter);
                                    c.setKeyResolver(ipKeyResolver);
                                    c.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                                }))
                        .uri("lb://recommendation-service"))

                .route("recommendation-feed-route", r -> r
                        .path("/api/recommendation/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(authenticatedRateLimiter);
                                    c.setKeyResolver(ipKeyResolver);
                                    c.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                                }))
                        .uri("lb://recommendation-service"))

                .route("post-recommendations-route", r -> r
                        .path("/api/recommendations/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(authenticatedRateLimiter);
                                    c.setKeyResolver(ipKeyResolver);
                                    c.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                                }))
                        .uri("lb://post-service"))

                // Feed Route
                .route("feed-route", r -> r
                        .path("/api/feed/**")
                        .filters(f -> f
                                .filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                .requestRateLimiter(c -> {
                                    c.setRateLimiter(authenticatedRateLimiter);
                                    c.setKeyResolver(ipKeyResolver);
                                    c.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                                }))
                        .uri("lb://recommendation-service"))

                .build();
    }
}
