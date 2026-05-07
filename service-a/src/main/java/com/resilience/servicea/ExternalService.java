package com.resilience.servicea;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service métier qui appelle une dépendance externe instable.
 * Le Circuit Breaker est appliqué via l'annotation @CircuitBreaker de Resilience4j.
 *
 * Comportement simulé :
 *  - Les appels 4, 5, 6 échouent (simule une panne réseau).
 *  - Après le seuil, le circuit s'ouvre et la méthode fallback est invoquée.
 */
@Service
public class ExternalService {

    private static final Logger log = LoggerFactory.getLogger(ExternalService.class);
    private static final String CB_NAME = "service-a-cb";

    // Compteur pour simuler des pannes intermittentes
    private final AtomicInteger callCount = new AtomicInteger(0);

    // Contrôle de simulation (exposé via endpoint pour les tests)
    private volatile boolean forceFailure = false;

    /**
     * Appel protégé par Circuit Breaker + Retry.
     * En cas d'ouverture du circuit, {@link #fallbackData} est appelé.
     */
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "fallbackData")
    @Retry(name = CB_NAME)
    public String fetchExternalData(String requestId) {
        int count = callCount.incrementAndGet();
        log.info("[ServiceA] Appel #{}  requestId={}", count, requestId);

        if (forceFailure) {
            log.warn("[ServiceA] Panne simulée sur l'appel #{}", count);
            throw new ExternalServiceException("Service externe indisponible (panne simulée)");
        }

        // Simulation d'un appel réseau (50ms)
        try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        return String.format("Données reçues de l'externe [appel=#%d, requestId=%s]", count, requestId);
    }

    /**
     * Méthode de fallback invoquée par Resilience4j quand le circuit est ouvert
     * ou quand l'appel lève une exception après épuisement des retries.
     */
    @SuppressWarnings("unused")
    private String fallbackData(String requestId, Throwable ex) {
        log.warn("[ServiceA][FALLBACK] Circuit ouvert — requestId={}, cause={}",
            requestId, ex.getMessage());
        return String.format("FALLBACK: données en cache pour requestId=%s [%s]",
            requestId, ex.getClass().getSimpleName());
    }

    public void setForceFailure(boolean value) {
        forceFailure = value;
        log.info("[ServiceA] forceFailure={}", value);
    }

    public boolean isForceFailure() { return forceFailure; }
    public int getCallCount()       { return callCount.get(); }
}
