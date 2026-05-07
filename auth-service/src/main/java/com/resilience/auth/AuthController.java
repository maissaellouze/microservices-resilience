package com.resilience.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Contrôleur d'authentification.
 *
 * POST /auth/login    → génère un token JWT
 * POST /auth/validate → valide un token JWT existant
 * GET  /auth/health   → health check
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final JwtService jwtService;

    // Utilisateurs en mémoire pour la démo (en prod → DB + BCrypt)
    private static final Map<String, String[]> USERS = Map.of(
        "admin",   new String[]{"adminpass", "ROLE_ADMIN"},
        "userA",   new String[]{"passA",     "ROLE_USER"},
        "userB",   new String[]{"passB",     "ROLE_USER"}
    );

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    // ── POST /auth/login ────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        log.info("Tentative de connexion pour l'utilisateur : {}", request.username());

        String[] userInfo = USERS.get(request.username());
        if (userInfo == null || !userInfo[0].equals(request.password())) {
            log.warn("Echec d'authentification pour : {}", request.username());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Identifiants invalides"));
        }

        String token = jwtService.generateToken(request.username(), userInfo[1]);
        log.info("Token généré pour : {} [role={}]", request.username(), userInfo[1]);

        return ResponseEntity.ok(Map.of(
            "token",    token,
            "type",     "Bearer",
            "username", request.username(),
            "role",     userInfo[1]
        ));
    }

    // ── POST /auth/validate ─────────────────────────────────────────────────
    @PostMapping("/validate")
    public ResponseEntity<?> validate(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "Token manquant"));
        }
        try {
            Claims claims = jwtService.validateToken(token);
            return ResponseEntity.ok(Map.of(
                "valid",    true,
                "username", claims.getSubject(),
                "role",     claims.get("role", String.class),
                "expiry",   claims.getExpiration().toString()
            ));
        } catch (JwtException e) {
            log.warn("Token invalide : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("valid", false, "error", e.getMessage()));
        }
    }

    // ── GET /auth/health ────────────────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "auth-service"));
    }

    // ── DTO ─────────────────────────────────────────────────────────────────
    public record LoginRequest(String username, String password) {}
}
