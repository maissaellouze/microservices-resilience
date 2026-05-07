package com.resilience.serviceb;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service de paiement protégé par le pattern Bulkhead.
 *
 * Deux types de Bulkhead sont disponibles dans Resilience4j :
 *  - SemaphoreBulkhead  → limite les appels concurrents (utilisé ici par défaut)
 *  - ThreadPoolBulkhead → pool de threads dédié (configuré via type=THREADPOOL)
 *
 * En cas de saturation, la méthode {@link #fallbackPayment} est invoquée.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String BH_NAME = "payment-bh";

    private final AtomicInteger callCount  = new AtomicInteger(0);
    private volatile long simulatedDelayMs = 200;  // délai simulé du traitement paiement

    /**
     * Traite un paiement. Protégé par Bulkhead (sémaphore).
     */
    @Bulkhead(name = BH_NAME, fallbackMethod = "fallbackPayment")
    public PaymentResult processPayment(PaymentRequest request) {
        int count = callCount.incrementAndGet();
        log.info("[ServiceB] Traitement paiement #{} — montant={} devise={}",
            count, request.amount(), request.currency());

        // Simulation du traitement (appel passerelle de paiement)
        try {
            Thread.sleep(simulatedDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String txId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[ServiceB] Paiement #{} validé — txId={}", count, txId);

        return new PaymentResult(
            txId,
            "SUCCESS",
            request.amount(),
            request.currency(),
            "Paiement traité avec succès"
        );
    }

    /**
     * Fallback invoqué quand le Bulkhead est saturé (BulkheadFullException).
     */
    @SuppressWarnings("unused")
    private PaymentResult fallbackPayment(PaymentRequest request,
                                          io.github.resilience4j.bulkhead.BulkheadFullException ex) {
        log.warn("[ServiceB][FALLBACK] Bulkhead saturé pour montant={} — cause={}",
            request.amount(), ex.getMessage());
        return new PaymentResult(
            "PENDING-" + System.currentTimeMillis(),
            "QUEUED",
            request.amount(),
            request.currency(),
            "Système chargé. Paiement mis en file d'attente."
        );
    }

    public void setSimulatedDelayMs(long delay) { simulatedDelayMs = delay; }
    public long getSimulatedDelayMs()           { return simulatedDelayMs; }
    public int  getCallCount()                  { return callCount.get(); }

    // ── DTOs ─────────────────────────────────────────────────────────────────
    public record PaymentRequest(String userId, BigDecimal amount, String currency, String description) {}

    public record PaymentResult(String transactionId, String status,
                                 BigDecimal amount, String currency, String message) {}
}
