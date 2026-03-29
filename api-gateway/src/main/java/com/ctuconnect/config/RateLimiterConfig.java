package com.ctuconnect.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Rate limiting configuration using Redis token-bucket algorithm.
 *
 * Two tiers:
 *   - Public endpoints (auth/register, auth/login): 20 req/s burst, 10 req/s sustained
 *   - Authenticated endpoints: 100 req/s burst, 50 req/s sustained
 *
 * Keys are resolved per remote IP address. In production behind a reverse proxy,
 * ensure X-Forwarded-For is trusted so the real client IP is used.
 */
@Configuration
public class RateLimiterConfig {

    /** Resolve rate-limit bucket by client IP address. */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(ip);
        };
    }

    /**
     * Limiter for public/unauthenticated routes (login, register).
     * replenishRate = sustained requests per second
     * burstCapacity  = maximum burst size (token bucket capacity)
     * requestedTokens = tokens consumed per request
     */
    @Bean
    public RedisRateLimiter publicRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }

    /**
     * Limiter for authenticated API routes.
     */
    @Bean
    @Primary
    public RedisRateLimiter authenticatedRateLimiter() {
        return new RedisRateLimiter(50, 100, 1);
    }
}
