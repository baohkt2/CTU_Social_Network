package com.ctuconnect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    // DISABLED: CORS is handled by individual services (SecurityConfig)
    // This prevents duplicate CORS headers which causes browser errors:
    // "The 'Access-Control-Allow-Origin' header contains multiple values"
    
    // If you need to enable Gateway CORS again:
    // 1. Uncomment the bean below
    // 2. Remove CORS configuration from all service SecurityConfig files
    
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow specific origins
        configuration.addAllowedOrigin("http://localhost:3000"); // Client frontend
        configuration.addAllowedOrigin("http://localhost:3001"); // Admin frontend
        configuration.addAllowedOrigin("http://localhost:18090"); // API Gateway
        configuration.addAllowedOrigin("http://localhost:8090"); // API Gateway internal
        
        // Allow all headers
        configuration.addAllowedHeader("*");

        // Allow all methods
        configuration.addAllowedMethod("*");

        // Allow credentials
        configuration.setAllowCredentials(true);

        // Set max age for preflight requests
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return new CorsWebFilter(source);
    }
}
