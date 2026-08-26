#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RED=$'\033[0;31m'; GREEN=$'\033[0;32m'; YELLOW=$'\033[1;33m'; BOLD=$'\033[1m'; RESET=$'\033[0m'

die() { printf '%sERROR: %s%s\n' "$RED" "$*" "$RESET" >&2; exit 1; }
conf_for() {
    case "$1" in
        dev)    echo "nginx.dev.conf" ;;
        static) echo "nginx.static.conf" ;;
        *)      die "mode tidak dikenal: $1 (pakai: dev | static)" ;;
    esac
}

command -v nginx >/dev/null 2>&1 || die "nginx tidak ditemukan di PATH"

run_nginx() {
    local mode="$1" action="$2" conf
    conf="$(conf_for "$mode")"
    [[ -f "$PROJECT_DIR/reverse-proxy/nginx/$conf" ]] || die "config tidak ditemukan: reverse-proxy/nginx/$conf"
    mkdir -p "$PROJECT_DIR/.assets/run/logs"

    case "$action" in
        start)
            printf '%s>> nginx -p %s/ -c reverse-proxy/nginx/%s%s\n' "$YELLOW" "$PROJECT_DIR" "$conf" "$RESET"
            nginx -p "$PROJECT_DIR/" -c "reverse-proxy/nginx/$conf"
            printf '%s✔ nginx started (%s) — listen :9000%s\n' "$GREEN" "$mode" "$RESET"
            ;;
        stop)
            printf '%s>> nginx -p %s/ -c reverse-proxy/nginx/%s -s stop%s\n' "$YELLOW" "$PROJECT_DIR" "$conf" "$RESET"
            nginx -p "$PROJECT_DIR/" -c "reverse-proxy/nginx/$conf" -s stop
            printf '%s✔ nginx stopped (%s)%s\n' "$GREEN" "$mode" "$RESET"
            ;;
        reload)
            printf '%s>> nginx -p %s/ -c reverse-proxy/nginx/%s -s reload%s\n' "$YELLOW" "$PROJECT_DIR" "$conf" "$RESET"
            nginx -p "$PROJECT_DIR/" -c "reverse-proxy/nginx/$conf" -s reload
            printf '%s✔ nginx reloaded (%s)%s\n' "$GREEN" "$mode" "$RESET"
            ;;
        *) die "aksi tidak dikenal: $action (pakai: start | stop | reload)" ;;
    esac
}

usage() {
    cat <<EOF
run-proxy.sh — launcher nginx (portabel, single-shot)

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
        # Mode interaktif satu kali
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
