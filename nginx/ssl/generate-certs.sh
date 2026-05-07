#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# Génération d'un certificat SSL auto-signé pour le développement local
# ─────────────────────────────────────────────────────────────────────────────
set -e

SSL_DIR="$(dirname "$0")"

echo ">>> Génération du certificat SSL auto-signé..."

openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout "$SSL_DIR/server.key" \
    -out    "$SSL_DIR/server.crt" \
    -subj   "/C=FR/ST=Ile-de-France/L=Paris/O=Resilience Demo/CN=localhost" \
    -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"

echo ">>> Certificat généré :"
echo "    Clé privée : $SSL_DIR/server.key"
echo "    Certificat : $SSL_DIR/server.crt"
openssl x509 -noout -subject -dates -in "$SSL_DIR/server.crt"
