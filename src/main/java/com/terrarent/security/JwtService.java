package com.terrarent.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.impl.DefaultClaims;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.access-token-expiration}")
    private long accessTokenExpiration; // in milliseconds

    @Value("${application.security.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration; // in milliseconds

    @Value("${application.security.jwt.allow-insecure-parse:false}")
    private boolean allowInsecureParse;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails, accessTokenExpiration);
    }

    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {
        return generateToken(extraClaims, userDetails, accessTokenExpiration);
    }

    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails, refreshTokenExpiration);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts
                    .parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            if (!allowInsecureParse) {
                throw e;
            }
            try {
                String[] parts = token.split("\\.");
                if (parts.length < 2) throw new IllegalArgumentException("Invalid JWT format");
                byte[] decoded = Decoders.BASE64URL.decode(parts[1]);
                ObjectMapper mapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> map = mapper.readValue(decoded, java.util.Map.class);
                return new DefaultClaims(map);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to parse JWT claims insecurely", ex);
            }
        }
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public boolean isAllowInsecureParse() {
        return allowInsecureParse;
    }

    /**
     * Public wrapper to extract claims (may use insecure parse if enabled).
     */
    public Claims extractAllClaimsPublic(String token) {
        return extractAllClaims(token);
    }
}
