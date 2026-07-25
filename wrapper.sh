#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(pwd)"
RUN_DIR="$ROOT_DIR/.assets/run"
LOG_DIR="$RUN_DIR/logs"

COMPOSE_FILE="$ROOT_DIR/docker-compose.yml"
PROXY_MODE="${PROXY:-dev}"
RUN_MODE="${RUN:-source}"
case "$PROXY_MODE" in
    dev) NGINX_CONF="$ROOT_DIR/reverse-proxy/nginx.dev.conf" ;;
    static) NGINX_CONF="$ROOT_DIR/reverse-proxy/nginx.static.conf" ;;
    *) printf 'ERROR: unknown proxy mode: %s \n' "$PROXY_MODE" >&2; exit 1 ;;
esac
case "$RUN_MODE" in
  source|build) ;;
  *) printf 'ERROR: unknown run mode: %s\n' "$RUN_MODE" >&2; exit 1 ;;
esac

mkdir -p "$RUN_DIR" "$LOG_DIR"

echo "$ROOT_DIR" "$RUN_MODE" "$PROXY_MODE" "$NGINX_CONF"
