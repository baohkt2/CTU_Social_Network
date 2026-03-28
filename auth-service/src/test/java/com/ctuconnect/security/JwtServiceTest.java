package com.ctuconnect.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JwtService.
 *
 * JwtService uses @Value fields; this test sets those values explicitly
 * to run without loading a full Spring context.
 */
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;
    private CustomUserPrincipal customPrincipal;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "XpExu6h1RJoY1qFZyLVzJbor/aYutNR2AD86ZM/tKqc=");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 86400000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpirationMs", 604800000L);

        userDetails = User.withUsername("test@ctu.edu.vn")
                .password("irrelevant")
                .authorities(Collections.emptyList())
                .build();

        customPrincipal = new CustomUserPrincipal(
                "user-uuid-123",
                "test@ctu.edu.vn",
                "hashed-password",
                "USER",
                true,
                Collections.emptyList()
        );
    }

    // ── Token generation ──────────────────────────────────────────────────────

    @Test
    @DisplayName("generateToken returns a non-blank JWT string")
    void generateToken_returnsNonBlankString() {
        String token = jwtService.generateToken(userDetails);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("generateToken embeds userId and role when principal is CustomUserPrincipal")
    void generateToken_embedsCustomClaims() {
        String token = jwtService.generateToken(customPrincipal);

        assertThat(jwtService.extractUserId(token)).isEqualTo("user-uuid-123");
        assertThat(jwtService.extractRole(token)).isEqualTo("USER");
    }

    @Test
    @DisplayName("generateRefreshToken returns a non-blank JWT string")
    void generateRefreshToken_returnsNonBlankString() {
        String token = jwtService.generateRefreshToken(userDetails);
        assertThat(token).isNotBlank();
    }

    // ── Claim extraction ──────────────────────────────────────────────────────

    @Test
    @DisplayName("extractUsername returns the subject set during generation")
    void extractUsername_returnsSubject() {
        String token = jwtService.generateToken(userDetails);
        assertThat(jwtService.extractUsername(token)).isEqualTo("test@ctu.edu.vn");
    }

    // ── Token validation ──────────────────────────────────────────────────────

    @Test
    @DisplayName("isTokenValid(token, userDetails) returns true for a freshly generated token")
    void isTokenValid_withMatchingUser_returnsTrue() {
        String token = jwtService.generateToken(userDetails);
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid(token, userDetails) returns false when username does not match")
    void isTokenValid_withDifferentUser_returnsFalse() {
        String token = jwtService.generateToken(userDetails);

        UserDetails otherUser = User.withUsername("other@ctu.edu.vn")
                .password("irrelevant")
                .authorities(Collections.emptyList())
                .build();

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid(token) returns true for a valid token")
    void isTokenValid_standalone_returnsTrue() {
        String token = jwtService.generateToken(userDetails);
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid(token) returns false for a tampered token")
    void isTokenValid_tamperedToken_returnsFalse() {
        String token = jwtService.generateToken(userDetails);
        // Flip the last character to corrupt the signature
        String tampered = token.substring(0, token.length() - 1) + "X";
        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid(token) returns false for a completely invalid string")
    void isTokenValid_garbage_returnsFalse() {
        assertThat(jwtService.isTokenValid("not.a.jwt")).isFalse();
    }

    @Test
    @DisplayName("isTokenExpired returns false for a freshly generated token")
    void isTokenExpired_freshToken_returnsFalse() {
        String token = jwtService.generateToken(userDetails);
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }
}
