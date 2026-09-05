# Docker — Cara Menjalankan Backend

> Menjalankan backend sebagai container: Postgres, Meilisearch, aplikasi
> Spring Boot, dan Nginx reverse proxy. Arsitektur modul ada di
> `ARCHITECTURE.md`, konfigurasi search di `SEARCH.md`.

---

## 1. Service

| Service | Image | Container | Port host | Keterangan |
|---|---|---|---|---|
| `postgres` | `postgres:16-alpine` | `cafe-postgres` | `${DB_EXTERNAL_PORT:-5432}` | Database utama, volume `postgres_data` |
| `meilisearch` | `getmeili/meilisearch:v1.53.1` | `cafe-meilisearch` | `${MEILI_EXTERNAL_PORT:-7700}` | Search engine, volume `meili_data`, `MEILI_ENV=production` |
| `backend` | Build dari `./core/Dockerfile` | `cafe-backend` | `${BACKEND_PORT:-8081}` | Spring Boot (`APP_ENV=docker`), menunggu postgres + meilisearch healthy |
| `nginx` | `nginx:1.27-alpine` | `cafe-nginx` | `${NGINX_PORT:-9000}` | Reverse proxy (`nginx.docker.conf`), menunggu backend healthy |

Alur request: `Client → nginx (:9000) → backend (:8081)`. Di dalam
compose, backend membaca config dari `./config` yang di-mount ke
`/app/config`. Healthcheck: `pg_isready` (postgres), `curl /health`
(meilisearch), `pgrep java.*app.jar` (backend).

---

## 2. Konfigurasi — Satu Sumber (SSOT)

Seluruh variabel berasal dari **`config/docker.env`** (contoh:
`config/docker.env.example`). Compose sendiri tidak menyimpan nilai —
jangan edit `docker-compose.yml` untuk urusan config.

Variabel penting:

| Var | Contoh | Dipakai oleh |
|---|---|---|
| `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `DATABASE_NAMES_CORE_BACKEND` | — | postgres + backend |
| `MEILISEARCH_API_KEY` | `rascal-cafe-meilisearch-docker-2026` | meilisearch (`MEILI_MASTER_KEY`) + backend (`api-key`) |
| `MEILISEARCH_URL` | `http://meilisearch:7700` | backend (lihat `SEARCH.md`) |
| `BACKEND_PORT`, `NGINX_PORT`, `DB_EXTERNAL_PORT`, `MEILI_EXTERNAL_PORT` | `8081`, `9000`, `5432`, `7700` | mapping port compose |
| `XENDIT_*`, `IMAGEKIT_*` | — | backend (payment & image) |
| `GITHUB_USER`, `GITHUB_TOKEN` | — | build arg backend (ambil dependency `id.rascal:filter` dari GitHub Packages) |

Untuk jalan native (tanpa docker), pakai `config/native.env` — backend
default ke `localhost:8081` dan Meilisearch `http://localhost:7700`.

---

## 3. Perintah

```bash
# Build + jalankan semua service
docker compose --env-file config/docker.env up -d --build

# Lihat status dan log
docker compose ps
docker compose logs -f backend

# Hentikan (data volume tetap ada)
docker compose down

# Hapus total termasuk volume (data postgres & meili hilang)
docker compose down -v
```

Build backend membutuhkan `GITHUB_TOKEN` (build arg) karena library
`id.rascal:filter` diambil dari GitHub Packages — pastikan variabel
tersebut terisi di environment atau `config/docker.env`.

---

## 4. Pengembangan Lokal (tanpa Docker)

Build multi-modul memakai Maven wrapper di `core/` (`./core/mvnw`).
`wrapper.sh` di root menyediakan menu interaktif dan subcommand:

```bash
./wrapper.sh                  # menu interaktif
./wrapper.sh install-all      # install semua modul ke ~/.m2
./wrapper.sh install-one menu-core   # install satu modul saja
./wrapper.sh run              # spring-boot:run modul aplikasi (core-app)
./wrapper.sh compile [MODULE] # compile satu modul
./wrapper.sh clean-install    # clean install semua modul
```

Catatan: `run` tanpa `-am` memakai modul sibling dari `~/.m2` hasil
install terakhir — jalankan `install-one`/`install-all` dulu setelah
mengubah modul lain agar perubahan terbawa. Untuk seed data dev, teruskan
argumen program ke aplikasi (lihat `ARCHITECTURE.md` bagian Seed Framework,
misalnya `--seed dev`).
