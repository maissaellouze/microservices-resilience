package com.resilience.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Service de gestion des tokens JWT.
 * Génère et valide des tokens signés avec HMAC-SHA256.
 */
@Service
public class JwtService {

    @Value("${jwt.secret:mySecretKeyForJWTThatIsAtLeast256BitsLong!}")
    private String secret;

    @Value("${jwt.expiration:86400000}")   // 24h par défaut
    private long expirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Génère un token JWT pour l'utilisateur donné.
     */
    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);

        return Jwts.builder()
            .claims(claims)
            .subject(username)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * Valide un token JWT et retourne les claims.
     * Lève une JwtException si le token est invalide ou expiré.
     */
    public Claims validateToken(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * Extrait le nom d'utilisateur depuis un token (sans vérification).
     */
    public String extractUsername(String token) {
        return validateToken(token).getSubject();
    }
}
