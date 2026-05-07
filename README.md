# Microservices Résilients — Spring Boot + Resilience4j + Nginx

Architecture microservices complète avec patterns de résilience.

## Architecture

```
Client
  │
  ▼ HTTPS (443)
┌─────────────────────────────────────────────────────┐
│  Nginx  — SSL/TLS · Cache · Rate Limiting · LB      │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP (8080)
                       ▼
┌─────────────────────────────────────────────────────┐
│  API Gateway  — Routage · JWT · Fallback · CB       │
└──────┬────────────────┬────────────────┬────────────┘
       │ :8081          │ :8082          │ :8083
       ▼                ▼                ▼
  Auth Service     Service A         Service B
  JWT Generator    Circuit Breaker   Bulkhead
```

## Composants

| Service       | Port | Pattern         | Rôle                                  |
|---------------|------|-----------------|---------------------------------------|
| Nginx         | 443  | Reverse Proxy   | SSL, Cache, Rate Limiting, LB         |
| API Gateway   | 8080 | Gateway         | Routage, JWT, Fallback                |
| Auth Service  | 8081 | –               | Génération tokens JWT                 |
| Service A     | 8082 | Circuit Breaker | Service métier protégé par CB         |
| Service B     | 8083 | Bulkhead        | Service paiement protégé par BH       |

## Démarrage rapide

### 1. Générer le certificat SSL
```bash
chmod +x nginx/ssl/generate-certs.sh
./nginx/ssl/generate-certs.sh
```

### 2. Compiler les services
```bash
mvn clean package -DskipTests
```

### 3. Lancer avec Docker Compose
```bash
docker-compose up --build
```

### 4. Tester

**Obtenir un token JWT :**
```bash
curl -k -X POST https://localhost/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"adminpass"}'
```

**Appeler Service A (Circuit Breaker) :**
```bash
TOKEN="<votre-token>"
curl -k https://localhost/api/service-a/data \
  -H "Authorization: Bearer $TOKEN"
```

**Appeler Service B (Bulkhead) :**
```bash
curl -k -X POST https://localhost/api/service-b/payment/simple \
  -H "Authorization: Bearer $TOKEN"
```

**Simuler une panne sur Service A :**
```bash
curl -X POST http://localhost:8082/test/failure?enabled=true
```

**Vérifier l'état du Circuit Breaker :**
```bash
curl http://localhost:8082/cb/status
```

**Vérifier l'état du Bulkhead :**
```bash
curl http://localhost:8083/bh/status
```

## Endpoints Actuator

| URL | Description |
|-----|-------------|
| `http://localhost:8082/actuator/health` | Santé + état CB |
| `http://localhost:8082/actuator/metrics` | Métriques Micrometer |
| `http://localhost:8083/actuator/health` | Santé + état Bulkhead |

## Structure du projet
```
ms-resilience/
├── pom.xml                 # Parent Maven
├── docker-compose.yml      # Stack complète
├── nginx/
│   ├── nginx.conf          # Config Nginx
│   └── ssl/
│       └── generate-certs.sh
├── auth-service/           # JWT (port 8081)
├── api-gateway/            # Spring Cloud Gateway (port 8080)
├── service-a/              # Circuit Breaker (port 8082)
└── service-b/              # Bulkhead (port 8083)
```
# microservices-resilience
