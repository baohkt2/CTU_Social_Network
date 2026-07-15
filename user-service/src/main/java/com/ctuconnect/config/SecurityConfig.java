package com.ctuconnect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cấu hình bảo mật Spring Security mới (Spring Security 6.x+)
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Tắt CSRF (thường dùng khi API)
                .authorizeHttpRequests(auth -> auth
                        // Cho phép truy cập công khai đến các endpoint internal service calls
                        .requestMatchers(
                                "/api/users/**",
                                "/actuator/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
        // Có thể thêm cấu hình login, logout, httpBasic, hoặc OAuth2 resource server nếu cần
        //.httpBasic(Customizer.withDefaults())
        //.formLogin(Customizer.withDefaults())
        ;

        return http.build();
    }
}
