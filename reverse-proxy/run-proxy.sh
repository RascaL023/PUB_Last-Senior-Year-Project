#!/usr/bin/env bash
set -Eeuo pipefail

# Launcher nginx portabel + interaktif (single-shot, tidak loop).
# Config di-render dari config/native.env (atau fallback config/native.env.example)
# via envsubst tanpa perlu yq/jq.
#
# Pemakaian:
#   ./reverse-proxy/run-proxy.sh                 # menu interaktif satu kali
#   ./reverse-proxy/run-proxy.sh dev             # dev + start (default action)
#   ./reverse-proxy/run-proxy.sh static stop
#   ./reverse-proxy/run-proxy.sh dev reload

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RED=$'\033[0;31m'; GREEN=$'\033[0;32m'; YELLOW=$'\033[1;33m'; BOLD=$'\033[1m'; RESET=$'\033[0m'

die() { printf '%sERROR: %s%s\n' "$RED" "$*" "$RESET" >&2; exit 1; }

command -v nginx    >/dev/null 2>&1 || die "nginx tidak ditemukan di PATH"
command -v envsubst >/dev/null 2>&1 || die "envsubst tidak ditemukan (butuh GNU gettext / envsubst)"

conf_for() {
    case "$1" in
        dev)    echo "nginx.dev.conf" ;;
        static) echo "nginx.static.conf" ;;
        *)      die "mode tidak dikenal: $1 (pakai: dev | static)" ;;
    esac
}
rendered_for() {
    case "$1" in
        dev)    echo "nginx.dev.conf.rendered" ;;
        static) echo "nginx.static.conf.rendered" ;;
        *)      die "mode tidak dikenal: $1 (pakai: dev | static)" ;;
    esac
}

# Ambil nilai env var dari native.env (atau fallback native.env.example)
get_env_val() {
    local key="$1" default_val="$2"
    local env_file="$PROJECT_DIR/config/native.env"
    if [[ ! -f "$env_file" ]]; then
        env_file="$PROJECT_DIR/config/native.env.example"
    fi

    if [[ -f "$env_file" ]]; then
        local val
        val="$(grep -v '^#' "$env_file" | grep "^${key}=" | head -n 1 | cut -d= -f2- | tr -d '"'\'' ' || true)"
        if [[ -n "$val" ]]; then
            echo "$val"
            return
        fi
    fi

    echo "$default_val"
}

# Render template -> file .rendered di folder yang sama (agar include mime.types tetap valid)
render_conf() {
    local mode="$1" template rendered
    template="$(conf_for "$mode")"
    rendered="$(rendered_for "$mode")"

    export NGINX_PORT="$(get_env_val "NGINX_PORT" "9000")"
    export SERVICES_CORE_PORT="$(get_env_val "SERVICES_CORE_PORT" "8081")"
    export SERVICES_FRONTEND_PORT="$(get_env_val "SERVICES_FRONTEND_PORT" "5173")"
    export FRONTEND_BUILD_DIR="$(get_env_val "FRONTEND_BUILD_DIR" "../frontend/build")"
    export NGINX_RATE_LIMIT="$(get_env_val "NGINX_RATE_LIMIT" "10r/s")"
    export NGINX_RATE_BURST="$(get_env_val "NGINX_RATE_BURST" "20")"
    export CORS_ORIGIN="$(get_env_val "SERVICES_FRONTEND_ORIGIN" "http://localhost:5173")"
    export DOLLAR='$'

    envsubst '$DOLLAR $CORS_ORIGIN $NGINX_PORT $SERVICES_CORE_PORT $SERVICES_FRONTEND_PORT $FRONTEND_BUILD_DIR $NGINX_RATE_LIMIT $NGINX_RATE_BURST' \
        < "$PROJECT_DIR/reverse-proxy/nginx/$template" \
        > "$PROJECT_DIR/reverse-proxy/nginx/$rendered"
}

run_nginx() {
    local mode="$1" action="$2" conf
    conf="$(rendered_for "$mode")"
    [[ -f "$PROJECT_DIR/reverse-proxy/nginx/$conf" ]] || render_conf "$mode"
    mkdir -p "$PROJECT_DIR/.assets/run/logs"

    case "$action" in
        start)
            render_conf "$mode"
            local proxy_port="$(get_env_val "NGINX_PORT" "9000")"
            printf '%s>> nginx -p %s/ -c reverse-proxy/nginx/%s%s\n' "$YELLOW" "$PROJECT_DIR" "$conf" "$RESET"
            nginx -p "$PROJECT_DIR/" -e "$PROJECT_DIR/.assets/run/logs/proxy-error.log" -c "reverse-proxy/nginx/$conf"
            printf '%s✔ nginx started (%s) — listen :%s%s\n' "$GREEN" "$mode" "$proxy_port" "$RESET"
            ;;
        stop)
            printf '%s>> nginx -p %s/ -c reverse-proxy/nginx/%s -s stop%s\n' "$YELLOW" "$PROJECT_DIR" "$conf" "$RESET"
            nginx -p "$PROJECT_DIR/" -e "$PROJECT_DIR/.assets/run/logs/proxy-error.log" -c "reverse-proxy/nginx/$conf" -s stop
            printf '%s✔ nginx stopped (%s)%s\n' "$GREEN" "$mode" "$RESET"
            ;;
        reload)
            render_conf "$mode"
            printf '%s>> nginx -p %s/ -c reverse-proxy/nginx/%s -s reload%s\n' "$YELLOW" "$PROJECT_DIR" "$conf" "$RESET"
            nginx -p "$PROJECT_DIR/" -e "$PROJECT_DIR/.assets/run/logs/proxy-error.log" -c "reverse-proxy/nginx/$conf" -s reload
            printf '%s✔ nginx reloaded (%s)%s\n' "$GREEN" "$mode" "$RESET"
            ;;
        *) die "aksi tidak dikenal: $action (pakai: start | stop | reload)" ;;
    esac
}

usage() {
    cat <<EOF
run-proxy.sh — launcher nginx (portabel, single-shot, render via envsubst)

Tanpa argumen   : menu interaktif (pilih mode + aksi, lalu keluar)
Subcommand:
  <mode> [aksi]   dev|static  lalu start|stop|reload (default: start)
                  contoh: run-proxy.sh dev stop
  -h|--help       Tampilkan bantuan ini
EOF
}

main() {
    local mode="${1:-}" action="${2:-}"

    if [[ -z "$mode" ]] || [[ "$mode" == "-h" || "$mode" == "--help" ]]; then
        [[ "$mode" == "-h" || "$mode" == "--help" ]] && { usage; exit 0; }
        printf '%sPilih mode proxy:%s\n' "$BOLD" "$RESET"
        printf '  %s1)%s dev     (proxy /api -> 8081, / -> Vite 5173)\n' "$GREEN" "$RESET"
        printf '  %s2)%s static  (proxy /api -> 8081, / -> frontend/build)\n' "$GREEN" "$RESET"
        local m; printf 'Masukkan pilihan [1-2]: '; read -r m
        case "$m" in
            1) mode="dev" ;;
            2) mode="static" ;;
            *) die "pilihan mode tidak valid: $m" ;;
        esac

        printf '%sPilih aksi:%s\n' "$BOLD" "$RESET"
        printf '  %s1)%s start   %s2)%s stop   %s3)%s reload%s\n' "$GREEN" "$RESET" "$GREEN" "$RESET" "$GREEN" "$RESET"
        local a; printf 'Masukkan pilihan [1-3]: '; read -r a
        case "$a" in
            1) action="start" ;;
            2) action="stop" ;;
            3) action="reload" ;;
            *) die "pilihan aksi tidak valid: $a" ;;
        esac
    fi

    action="${action:-start}"
    run_nginx "$mode" "$action"
}

main "$@"
