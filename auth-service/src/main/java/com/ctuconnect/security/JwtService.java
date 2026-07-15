package com.ctuconnect.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
// Important for handling exceptions in 0.12.x+
import io.jsonwebtoken.security.SignatureException; // Use this for invalid signatures
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.UnsupportedJwtException;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.Key; // java.security.Key is correct
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {


    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();

        // Add user ID and role to claims if userDetails is CustomUserPrincipal
        if (userDetails instanceof CustomUserPrincipal) {
            CustomUserPrincipal principal = (CustomUserPrincipal) userDetails;
            extraClaims.put("userId", principal.getUserId());
            extraClaims.put("role", principal.getRole());
        }

        return generateToken(extraClaims, userDetails);
    }

    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {
        // 24 giờ (tính bằng milliseconds)
        long jwtExpiration = 86400000;
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        // 7 ngày (tính bằng milliseconds)
        long refreshExpiration = 604800000;
        return buildToken(new HashMap<>(), userDetails, refreshExpiration);
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser() // Corrected: parser() instead of parserBuilder()
                .verifyWith((SecretKey) getSignInKey()) // Corrected: verifyWith() instead of setSigningKey()
                .build() // This build() call is still necessary after verifyWith()
                .parseSignedClaims(token) // Corrected: parseSignedClaims() instead of parseClaimsJws()
                .getPayload(); // Corrected: getPayload() instead of getBody()
    }

    public boolean isTokenValid(String token) { // Simplified validation for reusability if userDetails not needed
        try {
            Jwts.parser()
                    .verifyWith((SecretKey) getSignInKey())
                    .build()
                    .parseSignedClaims(token); // Just parsing to see if it's valid
            return true;
        } catch (SignatureException e) {
            System.err.println("Invalid JWT signature: " + e.getMessage());
        } catch (MalformedJwtException e) {
            System.err.println("Invalid JWT token: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            System.err.println("JWT token is expired: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.err.println("JWT token is unsupported: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("JWT claims string is empty: " + e.getMessage());
        }
        return false;
    }

    @org.springframework.beans.factory.annotation.Value("${jwt.secret:XpExu6h1RJoY1qFZyLVzJbor/aYutNR2AD86ZM/tKqc=}")
    private String secretKey;

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Removed getJwtFromRequest and getAuthentication as they seem to belong in a filter/util class,
    // not directly in JwtService which focuses on token operations.
    // If you need them here, ensure all relevant imports are present.
}