package com.resilience.servicea;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Contrôleur du Service A.
 *
 * GET  /data           → appel protégé par Circuit Breaker
 * GET  /health         → santé du service
 * POST /test/failure   → active/désactive la simulation de panne
 * GET  /cb/status      → état du Circuit Breaker
 */
@RestController
public class ServiceAController {

    private final ExternalService externalService;
    private final CircuitBreakerRegistry cbRegistry;

    public ServiceAController(ExternalService externalService,
                              CircuitBreakerRegistry cbRegistry) {
        this.externalService = externalService;
        this.cbRegistry      = cbRegistry;
    }
    @GetMapping("/hello") // Doit correspondre exactement au reste après le StripPrefix
    public String sayHello() {
        return "Hello from Service A!";
    }

    // ── GET /data ─────────────────────────────────────────────────────────────
    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> getData(
            @RequestHeader(value = "X-Auth-Username", defaultValue = "anonymous") String username,
            @RequestHeader(value = "X-Auth-Role",     defaultValue = "NONE")      String role) {

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        String result    = externalService.fetchExternalData(requestId);

        return ResponseEntity.ok(Map.of(
            "service",   "service-a",
            "user",      username,
            "role",      role,
            "requestId", requestId,
            "result",    result,
            "cbState",   getCbState()
        ));
    }

    // ── GET /health ───────────────────────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status",    "UP",
            "service",   "service-a",
            "cbState",   getCbState(),
            "callCount", externalService.getCallCount()
        ));
    }

    // ── POST /test/failure ────────────────────────────────────────────────────
    @PostMapping("/test/failure")
    public ResponseEntity<Map<String, Object>> setFailure(@RequestParam boolean enabled) {
        externalService.setForceFailure(enabled);
        return ResponseEntity.ok(Map.of(
            "forceFailure", enabled,
            "message", enabled ? "Simulation de panne ACTIVÉE" : "Simulation de panne DÉSACTIVÉE"
        ));
    }

    // ── GET /cb/status ────────────────────────────────────────────────────────
    @GetMapping("/cb/status")
    public ResponseEntity<Map<String, Object>> circuitBreakerStatus() {
        var cb      = cbRegistry.circuitBreaker("service-a-cb");
        var metrics = cb.getMetrics();
        return ResponseEntity.ok(Map.of(
            "name",               cb.getName(),
            "state",              cb.getState().toString(),
            "failureRate",        String.format("%.1f%%", metrics.getFailureRate()),
            "bufferedCalls",      metrics.getNumberOfBufferedCalls(),
            "failedCalls",        metrics.getNumberOfFailedCalls(),
            "successfulCalls",    metrics.getNumberOfSuccessfulCalls(),
            "notPermittedCalls",  metrics.getNumberOfNotPermittedCalls()
        ));
    }

    private String getCbState() {
        try {
            return cbRegistry.circuitBreaker("service-a-cb").getState().toString();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
