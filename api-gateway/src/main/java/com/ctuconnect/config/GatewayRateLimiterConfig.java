package com.ctuconnect.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayRateLimiterConfig {

    /**
     * Token bucket for unauthenticated auth endpoints (login/register/etc.): 30 req/s sustained, burst 60.
     */
    @Bean
    public RedisRateLimiter authPublicRedisRateLimiter() {
        return new RedisRateLimiter(30, 60, 1);
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            var remote = exchange.getRequest().getRemoteAddress();
            String key = remote != null && remote.getAddress() != null
                    ? remote.getAddress().getHostAddress()
                    : "unknown";
            return Mono.just(key);
        };
    }
}
