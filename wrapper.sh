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

require_git() {
    command -v git >/dev/null 2>&1 || die "git tidak ditemukan di PATH (dibutuhkan perintah ini)"
}

# State global untuk flag build (diisi parse_common_args / perintah run)
DRY_RUN=0
EXTRA_MAVEN=()
MVN_CMD=()
SEL_MODULES=""
AM_MODE=""
TESTS_MODE=""
SINGLE_TEST=""
FLAG_INCLUDES=""
FLAG_AM=1
FLAG_AMD=0
FLAG_CLEAN=0
FLAG_SKIP_TESTS=1
FLAG_OFFLINE=0
FLAG_QUIET=0

run_mvn() {
    if [[ "$DRY_RUN" == "1" ]]; then
        printf '%s>> (cd %s && ./mvnw %s)%s\n' "$BLUE" "$BACKEND_DIR" "$*" "$RESET"
        return 0
    fi
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

join_by_comma() {
    local IFS=,
    echo "$*"
}

# Resolve input modul (csv/spasi/'all') menjadi daftar dipisah spasi sesuai urutan reaktor
resolve_modules() {
    local input="${1:-}"
    [[ -n "$input" ]] || die "tentukan modul (csv, atau 'all')"
    if [[ "$input" == "all" ]]; then
        echo "${MODULES[*]}"
        return 0
    fi
    local -a req=()
    read -ra req <<< "${input//,/ }"
    [[ ${#req[@]} -gt 0 ]] || die "tidak ada modul valid: $input"
    local r m found
    for r in "${req[@]}"; do
        [[ -n "$r" ]] || continue
        found=0
        for m in "${MODULES[@]}"; do
            if [[ "$r" == "$m" ]]; then found=1; break; fi
        done
        [[ $found -eq 1 ]] || die "modul tidak dikenal: $r"
    done
    local -a out=()
    for m in "${MODULES[@]}"; do
        for r in "${req[@]}"; do
            if [[ "$r" == "$m" ]]; then out+=("$m"); break; fi
        done
    done
    [[ ${#out[@]} -gt 0 ]] || die "tidak ada modul valid: $input"
    echo "${out[*]}"
}

# Pilih modul interaktif: nomor/nama csv atau 'all'; output csv validasi via resolve
pick_modules() {
    local title="${1:-Pilih modul}"
    printf '\n%s%s:%s\n' "$BOLD" "$title" "$RESET" >&2
    local i
    for i in "${!MODULES[@]}"; do
        printf '  %s%d)%s %s%s%s\n' "$GREEN" $((i + 1)) "$RESET" "$BOLD" "${MODULES[$i]}" "$RESET" >&2
    done
    printf '  %sall)%s semua modul\n' "$GREEN" "$RESET" >&2
    local sel
    printf 'Pilih nomor/nama (koma) atau all: ' >&2
    read -r sel
    [[ -n "$sel" ]] || die "pilihan modul kosong"
    if [[ "$sel" == "all" ]]; then
        echo "${MODULES[*]}"
        return 0
    fi
    local -a tokens=()
    read -ra tokens <<< "${sel//,/ }"
    local -a names=()
    local t
    for t in "${tokens[@]}"; do
        if [[ "$t" =~ ^[0-9]+$ ]]; then
            (( t >= 1 && t <= ${#MODULES[@]} )) || die "pilihan modul tidak valid: $t"
            names+=("${MODULES[$((t - 1))]}")
        else
            names+=("$t")
        fi
    done
    resolve_modules "$(join_by_comma "${names[@]}")"
}

# Resolve input menjadi plist koma untuk -pl
resolve_plist() {
    local resolved=""
    resolved="$(resolve_modules "$1")" || return 1
    local -a mods=()
    read -ra mods <<< "$resolved"
    join_by_comma "${mods[@]}"
}

# Parser flag build standar; menghasilkan SEL_MODULES + FLAG_* + EXTRA_MAVEN
parse_common_args() {
    SEL_MODULES=""
    AM_MODE=""
    TESTS_MODE=""
    SINGLE_TEST=""
    FLAG_INCLUDES=""
    FLAG_AMD=0
    FLAG_CLEAN=0
    FLAG_OFFLINE=0
    FLAG_QUIET=0
    EXTRA_MAVEN=()
    local end_opts=0 a
    while [[ $# -gt 0 ]]; do
        a="$1"; shift
        if [[ $end_opts -eq 1 ]]; then EXTRA_MAVEN+=("$a"); continue; fi
        case "$a" in
            --) end_opts=1 ;;
            --dry-run) DRY_RUN=1 ;;
            --am) AM_MODE="on" ;;
            --no-am) AM_MODE="off" ;;
            --amd) FLAG_AMD=1 ;;
            --clean) FLAG_CLEAN=1 ;;
            --tests) TESTS_MODE="run" ;;
            --skip-tests) TESTS_MODE="skip" ;;
            --offline) FLAG_OFFLINE=1 ;;
            --quiet) FLAG_QUIET=1 ;;
            --test)
                [[ $# -gt 0 ]] || die "--test butuh nilai"
                SINGLE_TEST="$1"; shift ;;
            --test=*) SINGLE_TEST="${a#--test=}" ;;
            --includes)
                [[ $# -gt 0 ]] || die "--includes butuh nilai"
                FLAG_INCLUDES="$1"; shift ;;
            --includes=*) FLAG_INCLUDES="${a#--includes=}" ;;
            -h|--help) usage; exit 0 ;;
            --*) die "flag tidak dikenal: $a" ;;
            -*) EXTRA_MAVEN+=("$a") ;;
            *)
                if [[ -z "$SEL_MODULES" ]]; then
                    SEL_MODULES="$a"
                else
                    die "terlalu banyak argumen modul: $a"
                fi ;;
        esac
    done
}

apply_am_default() {
    local def="${1:-on}"
    if [[ -n "$AM_MODE" ]]; then
        if [[ "$AM_MODE" == "on" ]]; then FLAG_AM=1; else FLAG_AM=0; fi
    else
        if [[ "$def" == "on" ]]; then FLAG_AM=1; else FLAG_AM=0; fi
    fi
}

apply_tests_default() {
    local def="${1:-skip}"
    if [[ "$TESTS_MODE" == "run" ]]; then
        FLAG_SKIP_TESTS=0
    elif [[ "$TESTS_MODE" == "skip" ]]; then
        FLAG_SKIP_TESTS=1
    else
        if [[ "$def" == "run" ]]; then FLAG_SKIP_TESTS=0; else FLAG_SKIP_TESTS=1; fi
    fi
}

base_flags() {
    if [[ "$FLAG_OFFLINE" == "1" ]]; then MVN_CMD+=(-o); fi
    if [[ "$FLAG_QUIET" == "1" ]]; then MVN_CMD+=(-q); fi
}

# Susun perintah modul: build_mvn_cmd PLIST GOALS...
build_mvn_cmd() {
    local plist="$1"; shift
    [[ -n "$plist" ]] || die "internal: daftar modul kosong"
    MVN_CMD=(-pl "$plist")
    if [[ "$FLAG_AM" == "1" ]]; then MVN_CMD+=(-am); fi
    if [[ "$FLAG_AMD" == "1" ]]; then MVN_CMD+=(-amd); fi
    base_flags
    if [[ $# -gt 0 ]]; then MVN_CMD+=("$@"); fi
    if [[ ${#EXTRA_MAVEN[@]} -gt 0 ]]; then MVN_CMD+=("${EXTRA_MAVEN[@]}"); fi
}

announce_and_run() {
    printf '%s>> mvnw %s%s\n' "$BLUE" "${MVN_CMD[*]}" "$RESET"
    run_mvn "${MVN_CMD[@]}"
}

# Deteksi modul berubah dari git; output spasi-csv atau 'all'; mati bila nihil
detect_changed_modules() {
    local line path rest mod
    local saw_root=0
    local -a found=()
    while IFS= read -r line; do
        [[ -n "$line" ]] || continue
        path="${line:3}"
        if [[ "$path" == *" -> "* ]]; then path="${path##* -> }"; fi
        path="${path#\"}"; path="${path%\"}"
        if [[ "$path" == "core/pom.xml" || "$path" == "mvnw" || "$path" == "core/mvnw" || "$path" == .mvn/* || "$path" == "core/.mvn/"* ]]; then
            saw_root=1
            break
        fi
        if [[ "$path" == core/* ]]; then
            rest="${path#core/}"
            mod="${rest%%/*}"
            if [[ -n "$mod" && " ${MODULES[*]} " == *" $mod "* ]]; then
                found+=("$mod")
            fi
        fi
    done < <(git -C "$PROJECT_DIR" status --porcelain -- core mvnw .mvn 2>/dev/null || true)
    if [[ $saw_root -eq 1 ]]; then
        echo "all"
        return 0
    fi
    if [[ ${#found[@]} -eq 0 ]]; then
        die "tidak ada perubahan modul terdeteksi di core/"
    fi
    resolve_modules "$(join_by_comma "${found[@]}")"
}

header() {
    printf '\n%s%s %s %s\n' "$BOLD" "=============== $(basename "$PROJECT_DIR") — Modular Monolith ===============" "$RESET"
    printf '%sApp module : %s%s\n' "$YELLOW" "$APP_MODULE" "$RESET"
    printf '%sModules    : %s%s\n' "$YELLOW" "${MODULES[*]}" "$RESET"
}

pause() {
    printf '\n%sTekan Enter untuk melanjutkan...%s' "$BOLD" "$RESET"
    read -r
    clear
}

print_menu() {
    printf '\n%sPilih aksi:%s\n' "$BOLD" "$RESET"
    printf '  %s1)%s Install modul (satu/multi)\n' "$GREEN" "$RESET"
    printf '  %s2)%s Install semua modul\n' "$GREEN" "$RESET"
    printf '  %s3)%s Clean install semua modul\n' "$GREEN" "$RESET"
    printf '  %s4)%s Run aplikasi (%s)\n' "$GREEN" "$RESET" "$APP_MODULE"
    printf '  %s5)%s Compile modul\n' "$GREEN" "$RESET"
    printf '  %s6)%s Clean modul\n' "$GREEN" "$RESET"
    printf '  %s7)%s Test modul\n' "$GREEN" "$RESET"
    printf '  %s8)%s Package modul\n' "$GREEN" "$RESET"
    printf '  %s9)%s Verify modul\n' "$GREEN" "$RESET"
    printf '  %s10)%s Dependencies (resolve/tree/offline)\n' "$GREEN" "$RESET"
    printf '  %s11)%s Modul berubah & dependents\n' "$GREEN" "$RESET"
    printf '  %s12)%s Info & doctor (list/status/doctor)\n' "$GREEN" "$RESET"
    printf '  %s0)%s Quit\n' "$GREEN" "$RESET"
}

deps_menu() {
    while true; do
        printf '\n%sDependencies:%s\n' "$BOLD" "$RESET"
        printf '  %s1)%s Resolve\n' "$GREEN" "$RESET"
        printf '  %s2)%s Tree\n' "$GREEN" "$RESET"
        printf '  %s3)%s Go-offline\n' "$GREEN" "$RESET"
        printf '  %s0)%s Kembali\n' "$GREEN" "$RESET"
        local choice
        read -r -p "Masukkan pilihan [0-3]: " choice
        case "$choice" in
            1) cmd_deps ;;
            2) cmd_tree ;;
            3) cmd_offline ;;
            0) return 0 ;;
            *) printf '%sPilihan tidak dikenal: %s%s\n' "$RED" "$choice" "$RESET" ;;
        esac
        pause
    done
}

impact_menu() {
    while true; do
        printf '\n%sModul berubah & dependents:%s\n' "$BOLD" "$RESET"
        printf '  %s1)%s Install modul berubah (git)\n' "$GREEN" "$RESET"
        printf '  %s2)%s Rebuild dependents modul\n' "$GREEN" "$RESET"
        printf '  %s0)%s Kembali\n' "$GREEN" "$RESET"
        local choice
        read -r -p "Masukkan pilihan [0-2]: " choice
        case "$choice" in
            1) cmd_changed ;;
            2) cmd_dependents ;;
            0) return 0 ;;
            *) printf '%sPilihan tidak dikenal: %s%s\n' "$RED" "$choice" "$RESET" ;;
        esac
        pause
    done
}

info_menu() {
    while true; do
        printf '\n%sInfo & doctor:%s\n' "$BOLD" "$RESET"
        printf '  %s1)%s List modul\n' "$GREEN" "$RESET"
        printf '  %s2)%s Status\n' "$GREEN" "$RESET"
        printf '  %s3)%s Doctor\n' "$GREEN" "$RESET"
        printf '  %s0)%s Kembali\n' "$GREEN" "$RESET"
        local choice
        read -r -p "Masukkan pilihan [0-3]: " choice
        case "$choice" in
            1) cmd_list ;;
            2) cmd_status ;;
            3) cmd_doctor ;;
            0) return 0 ;;
            *) printf '%sPilihan tidak dikenal: %s%s\n' "$RED" "$choice" "$RESET" ;;
        esac
        pause
    done
}

reject_test_flags() {
    [[ -z "$SINGLE_TEST" ]] || die "--test hanya untuk perintah test"
    [[ -z "$FLAG_INCLUDES" ]] || die "--includes hanya untuk perintah tree"
}

cmd_install() {
    require_mvnw
    parse_common_args "$@"
    reject_test_flags
    apply_am_default on
    apply_tests_default skip
    local input="$SEL_MODULES"
    if [[ -z "$input" ]]; then
        input="$(pick_modules "Pilih modul untuk di-install")" || return 1
    fi
    local plist=""
    plist="$(resolve_plist "$input")" || return 1
    local -a goals=()
    if [[ "$FLAG_CLEAN" == "1" ]]; then goals+=(clean); fi
    goals+=(install)
    if [[ "$FLAG_SKIP_TESTS" == "1" ]]; then goals+=(-DskipTests); fi
    build_mvn_cmd "$plist" "${goals[@]}"
    announce_and_run
    printf '%s✔ install selesai: %s%s\n' "$GREEN" "${plist//,/ }" "$RESET"
}

cmd_clean() {
    require_mvnw
    parse_common_args "$@"
    reject_test_flags
    apply_am_default off
    local input="$SEL_MODULES"
    if [[ -z "$input" ]]; then
        input="$(pick_modules "Pilih modul untuk di-clean")" || return 1
    fi
    local plist=""
    plist="$(resolve_plist "$input")" || return 1
    build_mvn_cmd "$plist" clean
    announce_and_run
    printf '%s✔ clean selesai: %s%s\n' "$GREEN" "${plist//,/ }" "$RESET"
}

cmd_compile() {
    require_mvnw
    parse_common_args "$@"
    reject_test_flags
    apply_am_default on
    local input="$SEL_MODULES"
    if [[ -z "$input" ]]; then
        input="$(pick_modules "Pilih modul untuk di-compile")" || return 1
    fi
    local plist=""
    plist="$(resolve_plist "$input")" || return 1
    build_mvn_cmd "$plist" compile
    announce_and_run
    printf '%s✔ compile selesai: %s%s\n' "$GREEN" "${plist//,/ }" "$RESET"
}

cmd_test() {
    require_mvnw
    parse_common_args "$@"
    [[ -z "$FLAG_INCLUDES" ]] || die "--includes hanya untuk perintah tree"
    apply_am_default on
    apply_tests_default run
    local input="$SEL_MODULES"
    if [[ -z "$input" ]]; then
        input="$(pick_modules "Pilih modul untuk di-test")" || return 1
    fi
    local plist=""
    plist="$(resolve_plist "$input")" || return 1
    local -a goals=(test)
    if [[ -n "$SINGLE_TEST" ]]; then
        goals+=("-Dtest=$SINGLE_TEST" -DfailIfNoTests=false)
    fi
    build_mvn_cmd "$plist" "${goals[@]}"
    announce_and_run
    printf '%s✔ test selesai: %s%s\n' "$GREEN" "${plist//,/ }" "$RESET"
}

cmd_package() {
    require_mvnw
    parse_common_args "$@"
    reject_test_flags
    apply_am_default on
    apply_tests_default skip
    local input="$SEL_MODULES"
    if [[ -z "$input" ]]; then
        input="$(pick_modules "Pilih modul untuk di-package")" || return 1
    fi
    local plist=""
    plist="$(resolve_plist "$input")" || return 1
    local -a goals=()
    if [[ "$FLAG_CLEAN" == "1" ]]; then goals+=(clean); fi
    goals+=(package)
    if [[ "$FLAG_SKIP_TESTS" == "1" ]]; then goals+=(-DskipTests); fi
    build_mvn_cmd "$plist" "${goals[@]}"
    announce_and_run
    printf '%s✔ package selesai: %s%s\n' "$GREEN" "${plist//,/ }" "$RESET"
}

cmd_verify() {
    require_mvnw
    parse_common_args "$@"
    reject_test_flags
    apply_am_default on
    apply_tests_default skip
    local input="$SEL_MODULES"
    if [[ -z "$input" ]]; then
        input="$(pick_modules "Pilih modul untuk di-verify")" || return 1
    fi
    local plist=""
    plist="$(resolve_plist "$input")" || return 1
    local -a goals=()
    if [[ "$FLAG_CLEAN" == "1" ]]; then goals+=(clean); fi
    goals+=(verify)
    if [[ "$FLAG_SKIP_TESTS" == "1" ]]; then goals+=(-DskipTests); fi
    build_mvn_cmd "$plist" "${goals[@]}"
    announce_and_run
    printf '%s✔ verify selesai: %s%s\n' "$GREEN" "${plist//,/ }" "$RESET"
}

cmd_install_all() {
    require_mvnw
    parse_common_args "$@"
    reject_test_flags
    [[ -z "$SEL_MODULES" ]] || die "install-all tidak menerima modul (gunakan install)"
    apply_tests_default skip
    MVN_CMD=()
    base_flags
    MVN_CMD+=(install)
    if [[ "$FLAG_SKIP_TESTS" == "1" ]]; then MVN_CMD+=(-DskipTests); fi
    if [[ ${#EXTRA_MAVEN[@]} -gt 0 ]]; then MVN_CMD+=("${EXTRA_MAVEN[@]}"); fi
    announce_and_run
    printf '%s✔ Semua modul ter-install.%s\n' "$GREEN" "$RESET"
}

cmd_clean_install_all() {
    require_mvnw
    parse_common_args "$@"
    reject_test_flags
    [[ -z "$SEL_MODULES" ]] || die "clean-install tidak menerima modul (gunakan install --clean)"
    apply_tests_default skip
    MVN_CMD=()
    base_flags
    MVN_CMD+=(clean install)
    if [[ "$FLAG_SKIP_TESTS" == "1" ]]; then MVN_CMD+=(-DskipTests); fi
    if [[ ${#EXTRA_MAVEN[@]} -gt 0 ]]; then MVN_CMD+=("${EXTRA_MAVEN[@]}"); fi
    announce_and_run
    printf '%s✔ Clean install selesai.%s\n' "$GREEN" "$RESET"
}

cmd_deps() {
    require_mvnw
    parse_common_args "$@"
    reject_test_flags
    apply_am_default on
    local input="$SEL_MODULES"
    if [[ -z "$input" ]]; then
        input="$(pick_modules "Pilih modul untuk dependency:resolve")" || return 1
    fi
    local plist=""
    plist="$(resolve_plist "$input")" || return 1
    build_mvn_cmd "$plist" dependency:resolve
    announce_and_run
    printf '%s✔ dependency:resolve selesai: %s%s\n' "$GREEN" "${plist//,/ }" "$RESET"
}

cmd_tree() {
    require_mvnw
    parse_common_args "$@"
    [[ -z "$SINGLE_TEST" ]] || die "--test hanya untuk perintah test"
    apply_am_default on
    local input="$SEL_MODULES"
    if [[ -z "$input" ]]; then
        input="$(pick_modules "Pilih modul untuk dependency:tree")" || return 1
    fi
    local plist=""
    plist="$(resolve_plist "$input")" || return 1
    local -a goals=(dependency:tree)
    if [[ -n "$FLAG_INCLUDES" ]]; then
        goals+=("-Dincludes=$FLAG_INCLUDES")
    fi
    build_mvn_cmd "$plist" "${goals[@]}"
    announce_and_run
    printf '%s✔ dependency:tree selesai: %s%s\n' "$GREEN" "${plist//,/ }" "$RESET"
}

cmd_offline() {
    require_mvnw
    parse_common_args "$@"
    reject_test_flags
    apply_am_default on
    local input="$SEL_MODULES"
    if [[ -z "$input" ]]; then
        input="$(pick_modules "Pilih modul untuk dependency:go-offline")" || return 1
    fi
    local plist=""
    plist="$(resolve_plist "$input")" || return 1
    build_mvn_cmd "$plist" dependency:go-offline
    announce_and_run
    printf '%s✔ dependency:go-offline selesai: %s%s\n' "$GREEN" "${plist//,/ }" "$RESET"
}

cmd_dependents() {
    require_mvnw
    parse_common_args "$@"
    reject_test_flags
    apply_am_default on
    apply_tests_default skip
    FLAG_AMD=1
    local input="$SEL_MODULES"
    if [[ -z "$input" ]]; then
        input="$(pick_modules "Pilih modul untuk rebuild dependents")" || return 1
    fi
    local plist=""
    plist="$(resolve_plist "$input")" || return 1
    local -a goals=()
    if [[ "$FLAG_CLEAN" == "1" ]]; then goals+=(clean); fi
    goals+=(install)
    if [[ "$FLAG_SKIP_TESTS" == "1" ]]; then goals+=(-DskipTests); fi
    build_mvn_cmd "$plist" "${goals[@]}"
    announce_and_run
    printf '%s✔ dependents selesai: %s%s\n' "$GREEN" "${plist//,/ }" "$RESET"
}

cmd_changed() {
    require_mvnw
    require_git
    parse_common_args "$@"
    reject_test_flags
    apply_am_default on
    apply_tests_default skip
    FLAG_AMD=1
    local input=""
    if [[ -n "$SEL_MODULES" ]]; then
        input="$SEL_MODULES"
    else
        input="$(detect_changed_modules)" || exit $?
    fi
    local plist=""
    if [[ "$input" == "all" ]]; then
        plist="$(join_by_comma "${MODULES[@]}")"
    else
        plist="$(resolve_plist "$input")" || return 1
    fi
    local -a goals=()
    if [[ "$FLAG_CLEAN" == "1" ]]; then goals+=(clean); fi
    goals+=(install)
    if [[ "$FLAG_SKIP_TESTS" == "1" ]]; then goals+=(-DskipTests); fi
    build_mvn_cmd "$plist" "${goals[@]}"
    announce_and_run
    printf '%s✔ changed selesai: %s%s\n' "$GREEN" "${plist//,/ }" "$RESET"
}

cmd_list() {
    local i
    for i in "${!MODULES[@]}"; do
        if [[ "${MODULES[$i]}" == "$APP_MODULE" ]]; then
            printf '  %s%d)%s %s%s%s %s(app)%s\n' "$GREEN" $((i + 1)) "$RESET" "$BOLD" "${MODULES[$i]}" "$RESET" "$YELLOW" "$RESET"
        else
            printf '  %s%d)%s %s\n' "$GREEN" $((i + 1)) "$RESET" "${MODULES[$i]}"
        fi
    done
}

cmd_status() {
    printf '%sApp module : %s%s\n' "$YELLOW" "$APP_MODULE" "$RESET"
    printf '%sModules    : %s modul%s\n' "$YELLOW" "${#MODULES[@]}" "$RESET"
    if command -v git >/dev/null 2>&1; then
        local branch
        branch="$(git -C "$PROJECT_DIR" rev-parse --abbrev-ref HEAD 2>/dev/null || echo "?")"
        printf '%sGit branch : %s%s\n' "$YELLOW" "$branch" "$RESET"
        local changes
        changes="$(git -C "$PROJECT_DIR" status --short -- core 2>/dev/null | head -n 20 || true)"
        if [[ -z "$changes" ]]; then
            printf '%sCore       : bersih%s\n' "$GREEN" "$RESET"
        else
            printf '%sCore       : ada perubahan%s\n' "$YELLOW" "$RESET"
            printf '%s\n' "$changes"
        fi
    else
        printf '%sGit        : tidak tersedia%s\n' "$YELLOW" "$RESET"
    fi
    if [[ -f "$PROJECT_DIR/config/native.env" ]]; then
        printf '%sNative env : ada (config/native.env)%s\n' "$GREEN" "$RESET"
    else
        printf '%sNative env : tidak ada, fallback default/example%s\n' "$YELLOW" "$RESET"
    fi
}

cmd_doctor() {
    local fail=0
    if [[ -x "$MVNW" ]]; then
        printf '%s[ok] mvnw executable: %s%s\n' "$GREEN" "$MVNW" "$RESET"
    else
        printf '%s[fail] mvnw tidak executable: %s%s\n' "$RED" "$MVNW" "$RESET"
        fail=1
    fi
    if command -v java >/dev/null 2>&1; then
        printf '%s[ok] java: %s%s\n' "$GREEN" "$(java -version 2>&1 | head -n 1)" "$RESET"
    else
        printf '%s[fail] java tidak ditemukan di PATH%s\n' "$RED" "$RESET"
        fail=1
    fi
    if [[ ${#MODULES[@]} -gt 0 && -n "$APP_MODULE" ]]; then
        printf '%s[ok] %s modul terdeteksi, app: %s%s\n' "$GREEN" "${#MODULES[@]}" "$APP_MODULE" "$RESET"
    else
        printf '%s[fail] modul/app tidak terdeteksi%s\n' "$RED" "$RESET"
        fail=1
    fi
    if [[ -f "$PROJECT_DIR/config/native.env" ]]; then
        printf '%s[ok] config/native.env ada%s\n' "$GREEN" "$RESET"
    else
        printf '%s[warn] config/native.env tidak ada (pakai default/example)%s\n' "$YELLOW" "$RESET"
    fi
    if command -v git >/dev/null 2>&1; then
        printf '%s[ok] git tersedia%s\n' "$GREEN" "$RESET"
    else
        printf '%s[warn] git tidak tersedia (perintah changed/status terbatas)%s\n' "$YELLOW" "$RESET"
    fi
    local m2="$HOME/.m2"
    if [[ -d "$m2" ]]; then
        if [[ -w "$m2" ]]; then
            printf '%s[ok] %s writable%s\n' "$GREEN" "$m2" "$RESET"
        else
            printf '%s[warn] %s tidak writable%s\n' "$YELLOW" "$m2" "$RESET"
        fi
    else
        printf '%s[info] %s belum ada (dibuat otomatis saat build)%s\n' "$YELLOW" "$m2" "$RESET"
    fi
    return $fail
}

run_app() {
    require_mvnw
    local -a maven_opts=()
    local -a app_args=()
    local seen_sep=0 a
    while [[ $# -gt 0 ]]; do
        a="$1"; shift
        if [[ "$a" == "--" && $seen_sep -eq 0 ]]; then
            seen_sep=1
            continue
        fi
        if [[ $seen_sep -eq 0 ]]; then
            if [[ "$a" == "--dry-run" ]]; then
                DRY_RUN=1
                continue
            fi
            maven_opts+=("$a")
        else
            app_args+=("$a")
        fi
    done
    # Tanpa -am: modul sibling dipakai dari ~/.m2 (hasil install terakhir).
    # Jadi jalankan "install" dulu untuk merefleksikan perubahan.
    MVN_CMD=(-pl "$APP_MODULE" spring-boot:run)
    if [[ ${#maven_opts[@]} -gt 0 ]]; then MVN_CMD+=("${maven_opts[@]}"); fi
    if [[ ${#app_args[@]} -gt 0 ]]; then
        local IFS=' '
        MVN_CMD+=("-Dspring-boot.run.arguments=${app_args[*]}")
    fi
    announce_and_run
}

interactive() {
    require_mvnw
    header
    while true; do
        print_menu
        local choice
        read -r -p "Masukkan pilihan [0-12]: " choice
        case "$choice" in
            1) cmd_install ;;
            2) cmd_install_all ;;
            3) cmd_clean_install_all ;;
            4) run_app ;;
            5) cmd_compile ;;
            6) cmd_clean ;;
            7) cmd_test ;;
            8) cmd_package ;;
            9) cmd_verify ;;
            10) deps_menu ;;
            11) impact_menu ;;
            12) info_menu ;;
            0) printf '%sBye.%s\n' "$GREEN" "$RESET"; exit 0 ;;
            *) printf '%sPilihan tidak dikenal: %s%s\n' "$RED" "$choice" "$RESET" ;;
        esac
        pause
    done
}

usage() {
    cat <<EOF
wrapper.sh — interaktif runner untuk modular monolith

Tanpa argumen   : menu interaktif
Subcommand:
  install [MODULES] [flags]   Install modul, csv atau 'all' (default -am, skip tests)
  install-one [MODULE]        Alias install (kompatibel)
  install-all [flags]         Install semua modul
  clean-install [flags]       Clean install semua modul
  clean [MODULES] [flags]     Clean modul terpilih
  compile [MODULES] [flags]   Compile modul (default -am)
  test [MODULES] [--test N]   Test modul (default jalan, -am)
  package [MODULES] [flags]   Package modul
  verify [MODULES] [flags]    Verify modul
  deps [MODULES|all]          dependency:resolve
  tree MODULE [--includes P]  dependency:tree
  offline [MODULES|all]       dependency:go-offline
  dependents MODUL [flags]    Rebuild modul + downstream (-amd)
  changed [flags]             Install modul berubah (git) + downstream (-amd)
  list | status | doctor      Info & diagnosis native
  run [MAVEN_OPTS...] [-- APP_ARGS...]   Jalankan $APP_MODULE (spring-boot:run)

Flags build: --am --no-am --amd --clean --tests --skip-tests --offline --quiet
             --test NAMA (test saja) --includes POLA (tree saja) --dry-run
             -- EXTRA_MAVEN_ARGS (diteruskan ke mvnw)

Contoh:
  install auth-api,auth-core
  install --clean order-core
  test auth-core --test JwtAuthFilterTest
  tree auth-core --includes id.my.rascal:*
  changed --tests
  run -- --seed formal
EOF
}

main() {
    if [[ "${1:-}" == "--dry-run" ]]; then
        DRY_RUN=1
        shift
    fi
    local cmd="${1:-interactive}"
    if [[ $# -gt 0 ]]; then shift; fi
    case "$cmd" in
        interactive) interactive ;;
        install|install-one) cmd_install "$@" ;;
        install-all) cmd_install_all "$@" ;;
        clean-install) cmd_clean_install_all "$@" ;;
        clean) cmd_clean "$@" ;;
        compile) cmd_compile "$@" ;;
        test) cmd_test "$@" ;;
        package) cmd_package "$@" ;;
        verify) cmd_verify "$@" ;;
        deps) cmd_deps "$@" ;;
        tree) cmd_tree "$@" ;;
        offline) cmd_offline "$@" ;;
        dependents) cmd_dependents "$@" ;;
        changed) cmd_changed "$@" ;;
        list) cmd_list ;;
        status) cmd_status ;;
        doctor) cmd_doctor ;;
        run) run_app "$@" ;;
        -h|--help|help) usage ;;
        *) die "subcommand tidak dikenal: $cmd (lihat: $0 --help)" ;;
    esac
}

main "$@"
