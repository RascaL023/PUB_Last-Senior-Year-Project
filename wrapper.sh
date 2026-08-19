#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$PROJECT_DIR/core"
MVNW="$BACKEND_DIR/mvnw"

RED=$'\033[0;31m'
GREEN=$'\033[0;32m'
YELLOW=$'\033[1;33m'
BLUE=$'\033[1;34m'
BOLD=$'\033[1m'
RESET=$'\033[0m'

die() { printf '%sERROR: %s%s\n' "$RED" "$*" "$RESET" >&2; exit 1; }

require_mvnw() {
    [[ -x "$MVNW" ]] || die "mvnw tidak ditemukan: $MVNW"
}

run_mvn() {
    (
        cd "$BACKEND_DIR"
        exec ./mvnw "$@"
    )
}

# Deteksi modul dari backend/pom.xml (urutan reaktor sesuai deklarasi)
readarray -t MODULES < <(sed -n 's:.*<module>\([^<]*\)</module>.*:\1:p' "$BACKEND_DIR/pom.xml")
[[ ${#MODULES[@]} -gt 0 ]] || die "tidak ada modul ditemukan di backend/pom.xml"

# Modul yang punya spring-boot-maven-plugin = app yang bisa dijalankan
APP_MODULE=""
for m in "${MODULES[@]}"; do
    if grep -q 'spring-boot-maven-plugin' "$BACKEND_DIR/$m/pom.xml" 2>/dev/null; then
        APP_MODULE="$m"
        break
    fi
done
[[ -n "$APP_MODULE" ]] || APP_MODULE="${MODULES[${#MODULES[@]}-1]}"

header() {
    printf '\n%s%s %s %s\n' "$BOLD" "=============== $(basename "$PROJECT_DIR") — Modular Monolith ===============" "$RESET"
    printf '%sApp module : %s%s\n' "$YELLOW" "$APP_MODULE" "$RESET"
    printf '%sModules    : %s%s\n' "$YELLOW" "${MODULES[*]}" "$RESET"
}

print_menu() {
    printf '\n%sPilih aksi:%s\n' "$BOLD" "$RESET"
    printf '  %s1)%s Install satu modul (partial install)\n' "$GREEN" "$RESET"
    printf '  %s2)%s Install semua modul\n' "$GREEN" "$RESET"
    printf '  %s3)%s Clean install semua modul\n' "$GREEN" "$RESET"
    printf '  %s4)%s Run aplikasi (%s)\n' "$GREEN" "$RESET" "$APP_MODULE"
    printf '  %s5)%s Compile satu modul\n' "$GREEN" "$RESET"
    printf '  %s0)%s Quit\n' "$GREEN" "$RESET"
}

pick_module() {
    local title="$1"
    printf '\n%s%s:%s\n' "$BOLD" "$title" "$RESET" >&2
    local i
    for i in "${!MODULES[@]}"; do
        printf '  %s%d)%s %s%s%s\n' "$GREEN" $((i + 1)) "$RESET" "$BOLD" "${MODULES[$i]}" "$RESET" >&2
    done
    local sel
    printf 'Pilih nomor modul [1-%s]: ' "${#MODULES[@]}" >&2
    read -r sel
    [[ "$sel" =~ ^[0-9]+$ ]] && (( sel >= 1 && sel <= ${#MODULES[@]} )) || die "pilihan modul tidak valid: $sel"
    echo "${MODULES[$((sel - 1))]}"
}

install_one() {
    local mod="${1:-}"
    if [[ -z "$mod" ]]; then
        mod="$(pick_module "Pilih modul untuk di-install")"
    fi
    if [[ "$mod" == "$APP_MODULE" ]]; then
        # App butuh dependensi internal; -am ikutkan dependensi yang belum ter-install
        printf '%s>> mvnw -pl %s -am install -DskipTests%s\n' "$BLUE" "$mod" "$RESET"
        run_mvn -pl "$mod" -am install -DskipTests
    else
        printf '%s>> mvnw -pl %s install -DskipTests%s\n' "$BLUE" "$mod" "$RESET"
        run_mvn -pl "$mod" install -DskipTests
    fi
    printf '%s✔ %s ter-install.%s\n' "$GREEN" "$mod" "$RESET"
}

install_all() {
    printf '%s>> mvnw install -DskipTests%s\n' "$BLUE" "$RESET"
    run_mvn install -DskipTests
    printf '%s✔ Semua modul ter-install.%s\n' "$GREEN" "$RESET"
}

clean_install_all() {
    printf '%s>> mvnw clean install -DskipTests%s\n' "$BLUE" "$RESET"
    run_mvn clean install -DskipTests
    printf '%s✔ Clean install selesai.%s\n' "$GREEN" "$RESET"
}

run_app() {
    # Tanpa -am: modul sibling dipakai dari ~/.m2 (hasil install terakhir).
    # Jadi jalankan "install satu modul" dulu untuk merefleksikan perubahan.
    printf '%s>> mvnw -pl %s spring-boot:run%s\n' "$BLUE" "$APP_MODULE" "$RESET"
    run_mvn -pl "$APP_MODULE" spring-boot:run "$@"
}

compile_one() {
    local mod="${1:-}"
    if [[ -z "$mod" ]]; then
        mod="$(pick_module "Pilih modul untuk di-compile")"
    fi
    printf '%s>> mvnw -pl %s compile%s\n' "$BLUE" "$mod" "$RESET"
    run_mvn -pl "$mod" compile
    printf '%s✔ %s ter-compile.%s\n' "$GREEN" "$mod" "$RESET"
}

interactive() {
    require_mvnw
    header
    while true; do
        print_menu
        local choice
        read -r -p "Masukkan pilihan [0-5]: " choice
        case "$choice" in
            1) install_one ;;
            2) install_all ;;
            3) clean_install_all ;;
            4) run_app ;;
            5) compile_one ;;
            0) printf '%sBye.%s\n' "$GREEN" "$RESET"; exit 0 ;;
            *) printf '%sPilihan tidak dikenal: %s%s\n' "$RED" "$choice" "$RESET" ;;
        esac
        printf '\n%sTekan Enter untuk melanjutkan...%s' "$BOLD" "$RESET"
        read -r
        clear
    done
}

usage() {
    cat <<EOF
wrapper.sh — interaktif runner untuk modular monolith

Tanpa argumen   : menu interaktif
Subcommand:
  install-one [MODULE]   Install satu modul (partial install)
  install-all            Install semua modul
  clean-install          Clean install semua modul
  run [MAVEN_ARGS...]    Jalankan $APP_MODULE (spring-boot:run)
  compile [MODULE]       Compile satu modul
EOF
}

main() {
    require_mvnw
    local cmd="${1:-interactive}"
    shift 2>/dev/null || true
    case "$cmd" in
        interactive) interactive ;;
        install-one)
            [[ $# -le 1 ]] || die "terlalu banyak argumen untuk install-one"
            if [[ $# -eq 1 ]]; then
                [[ " ${MODULES[*]} " == *" $1 "* ]] || die "modul tidak dikenal: $1"
                install_one "$1"
            else
                install_one
            fi
            ;;
        install-all) install_all ;;
        clean-install) clean_install_all ;;
        run) run_app "$@" ;;
        compile)
            [[ $# -le 1 ]] || die "terlalu banyak argumen untuk compile"
            if [[ $# -eq 1 ]]; then
                [[ " ${MODULES[*]} " == *" $1 "* ]] || die "modul tidak dikenal: $1"
                compile_one "$1"
            else
                compile_one
            fi
            ;;
        -h|--help|help) usage ;;
        *) die "subcommand tidak dikenal: $cmd (lihat: $0 --help)" ;;
    esac
}

main "$@"
