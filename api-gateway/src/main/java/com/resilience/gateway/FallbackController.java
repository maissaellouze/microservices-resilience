package com.resilience.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Contrôleur de fallback appelé par Spring Cloud Gateway
 * quand un Circuit Breaker s'ouvre ou qu'un service est indisponible.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    @GetMapping("/service-a")
    public ResponseEntity<Map<String, Object>> fallbackServiceA() {
        log.warn("[Fallback] Service-A indisponible — réponse de secours retournée");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
            "status",    "DEGRADED",
            "service",   "service-a",
            "message",   "Service A temporairement indisponible. Veuillez réessayer dans quelques instants.",
            "timestamp", Instant.now().toString(),
            "fallback",  true
        ));
    }

    @GetMapping("/service-b")
    public ResponseEntity<Map<String, Object>> fallbackServiceB() {
        log.warn("[Fallback] Service-B indisponible — réponse de secours retournée");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
            "status",    "DEGRADED",
            "service",   "service-b",
            "message",   "Service B (paiements) temporairement indisponible.",
            "timestamp", Instant.now().toString(),
            "fallback",  true
        ));
    }
}
