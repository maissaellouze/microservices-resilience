package com.resilience.serviceb;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Contrôleur du Service B (Paiements).
 *
 * POST /payment        → traitement paiement (protégé par Bulkhead)
 * GET  /health         → santé du service
 * POST /test/delay     → modifier le délai de traitement simulé
 * GET  /bh/status      → état du Bulkhead
 */
@RestController
public class ServiceBController {

    private final PaymentService  paymentService;
    private final BulkheadRegistry bulkheadRegistry;

    public ServiceBController(PaymentService paymentService,
                              BulkheadRegistry bulkheadRegistry) {
        this.paymentService   = paymentService;
        this.bulkheadRegistry = bulkheadRegistry;
    }

    // ── POST /payment ─────────────────────────────────────────────────────────
    @PostMapping("/payment")
    public ResponseEntity<PaymentService.PaymentResult> processPayment(
            @RequestBody PaymentService.PaymentRequest request,
            @RequestHeader(value = "X-Auth-Username", defaultValue = "anonymous") String username) {

        PaymentService.PaymentResult result = paymentService.processPayment(request);
        return ResponseEntity.ok(result);
    }

    // ── GET /payment/simple (démo GET) ────────────────────────────────────────
    @GetMapping("/payment/simple")
    public ResponseEntity<PaymentService.PaymentResult> simplePayment(
            @RequestHeader(value = "X-Auth-Username", defaultValue = "anonymous") String username) {

        PaymentService.PaymentRequest req = new PaymentService.PaymentRequest(
            username, BigDecimal.valueOf(99.99), "EUR", "Paiement de démo");
        PaymentService.PaymentResult result = paymentService.processPayment(req);
        return ResponseEntity.ok(result);
    }

    // ── GET /health ───────────────────────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        var bh = getBulkheadInfo();
        return ResponseEntity.ok(Map.of(
            "status",    "UP",
            "service",   "service-b",
            "bulkhead",  bh,
            "callCount", paymentService.getCallCount()
        ));
    }

    // ── POST /test/delay ──────────────────────────────────────────────────────
    @PostMapping("/test/delay")
    public ResponseEntity<Map<String, Object>> setDelay(@RequestParam long ms) {
        paymentService.setSimulatedDelayMs(ms);
        return ResponseEntity.ok(Map.of(
            "simulatedDelayMs", ms,
            "message", "Délai de traitement mis à jour à " + ms + "ms"
        ));
    }

    // ── GET /bh/status ────────────────────────────────────────────────────────
    @GetMapping("/bh/status")
    public ResponseEntity<Map<String, Object>> bulkheadStatus() {
        return ResponseEntity.ok(getBulkheadInfo());
    }

    private Map<String, Object> getBulkheadInfo() {
        try {
            var bh      = bulkheadRegistry.bulkhead("payment-bh");
            var metrics = bh.getMetrics();
            return Map.of(
                "name",                bh.getName(),
                "availableConcurrentCalls", metrics.getAvailableConcurrentCalls(),
                "maxAllowedConcurrentCalls", metrics.getMaxAllowedConcurrentCalls()
            );
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }
}
