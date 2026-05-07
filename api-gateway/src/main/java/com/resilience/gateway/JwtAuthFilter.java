package com.resilience.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Filtre Gateway qui valide le token JWT sur chaque requête protégée.
 *
 * Si le header Authorization est absent ou le token invalide → 401 Unauthorized.
 * Sinon, les claims (username, role) sont ajoutés aux headers downstream.
 *
 * Usage dans application.yml :
 * <pre>
 *   filters:
 *     - JwtAuthFilter
 * </pre>
 */
@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.secret:mySecretKeyForJWTThatIsAtLeast256BitsLong!ChangeInProduction}")
    private String secret;

    public JwtAuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                log.warn("[Gateway] Token absent sur : {}", exchange.getRequest().getPath());
                return unauthorized(exchange, "Token JWT manquant ou mal formé");
            }

            String token = authHeader.substring(BEARER_PREFIX.length());

            try {
                Claims claims = parseToken(token);
                log.info("[Gateway] JWT valide pour : {} [role={}]",
                    claims.getSubject(), claims.get("role"));

                // Propager les informations utilisateur aux services en aval
                ServerWebExchange enriched = exchange.mutate()
                    .request(r -> r
                        .header("X-Auth-Username", claims.getSubject())
                        .header("X-Auth-Role", String.valueOf(claims.get("role")))
                    )
                    .build();

                return chain.filter(enriched);

            } catch (JwtException e) {
                log.warn("[Gateway] Token invalide : {}", e.getMessage());
                return unauthorized(exchange, "Token JWT invalide : " + e.getMessage());
            }
        };
    }

    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("X-Auth-Error", message);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        // Configurable depuis application.yml si besoin
    }
}
