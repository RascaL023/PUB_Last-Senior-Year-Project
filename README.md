# Hysteria Cafe — Backend

REST API untuk sistem kasir / operasional cafe: katalog menu, order, dining (meja), invoice & payment (Xendit), customer member, kitchen board, report dashboard, upload gambar (ImageKit), dan notifikasi email (Resend).

Arsitektur **modular monolith** Spring Boot — kode diorganisir per domain seperti microservice, di-deploy sebagai satu aplikasi.

> Dokumentasi kontrak API lengkap: [`API_CONTRACT.md`](./API_CONTRACT.md) · Auth & otorisasi: [`AUTH.md`](./AUTH.md) · Arsitektur modul: [`ARCHITECTURE.md`](./ARCHITECTURE.md) · Cara jalan Docker: [`DOCKER-SETUP.md`](./DOCKER-SETUP.md)

---

## Fitur Utama

- **Auth JWT + refresh rotation** — access token 15 menit (`Authorization: Bearer`), refresh token 20 hari via cookie `httpOnly`.
- **RBAC granular** — `@PreAuthorize("hasAuthority('...')")`, bukan cek role string. Role bawaan: `ADMIN`, `CASHIER`, `WAITER`, `KITCHEN`, `CUSTOMER_BASE`.
- **Menu + search** — CRUD menu/kategori/modifier, read projection Meilisearch dengan fallback otomatis ke PostgreSQL. `GET /menus` publik, varian cached di `/api/v2/menus`.
- **Order** — flow `CREATED → CONFIRMED → PREPARING → READY → COMPLETED` (+ `CANCELLED`), filter status multi-nilai, reconcile items.
- **Dining / meja** — open/close sesi, tambah order ke sesi, guest access via `guestToken`/`guestCode` tanpa login, tracking tamu, member attach via `/my/dinings`.
- **Invoice & payment** — satu-satunya jalur uang `POST /payments` (target selalu invoice). Settlement via `PaymentSettledEvent`. Provider `INTERNAL` (tunai) dan `XENDIT` (webhook server-to-server). Tidak ada refund (MVP: `PAID` bersifat final).
- **Customer member** — registrasi publik, profil `/customers/me`, validasi `customerId` server-side.
- **Kitchen board** — `GET /kitchen/orders` (tiket + `tableNumber`).
- **Report** — `GET /reports/dashboard/summary` (baca live via contract, hanya `ADMIN` & `CASHIER`).
- **Image** — client-side upload ke ImageKit, backend hanya menerbitkan kredensial signed (`GET /images/auth`) + webhook.
- **Password reset** — `forgot-password` / `reset-password` via email (Resend).

---

## Tech Stack

| Lapisan | Teknologi |
|---|---|
| Bahasa / Framework | Java 21, Spring Boot 4.0.0, Spring Data JPA, Spring Security, Validation |
| Database | PostgreSQL 16 (`postgres:16-alpine`) |
| Search | Meilisearch `v1.53.1` |
| Reverse proxy | Nginx `1.27-alpine` (rate limit 10 r/s, burst 20) |
| Build | Maven multi-modul (`core/pom.xml`, 29 modul) |
| Infra lokal | Docker Compose (postgres, meilisearch, backend, nginx) |
| Integrasi | Xendit (payment), ImageKit (upload), Resend (email) |
| Security filter | Library eksternal `id.rascal:filter` via GitHub Packages |

---

## Arsitektur

Setiap domain dipisah menjadi `*-api` (contract: interface + DTO, tanpa implementasi) dan `*-core` (implementasi). Aturan keras: **`*-core` tidak boleh depend langsung ke `*-core` lain — hanya ke `common` + `*-api`**.

```
Client → Nginx (:9000) → Backend (:8081) → PostgreSQL / Meilisearch
                                     ↘ Xendit / ImageKit / Resend (eksternal)
```

Modul (lihat `core/pom.xml`):

| Modul | Isi |
|---|---|
| `common` | Format response, exception handler, seed framework, util |
| `auth-api` / `auth-core` | User, role, authority, refresh token, `SecurityConfig` |
| `menu-api` / `menu-core` | Menu, kategori, modifier, admin search |
| `order-api` / `order-core` | Order, kitchen ticket, guest tracking |
| `invoice-api` / `invoice-core` | Billing aggregate, listener settlement |
| `payment-api` / `payment-core` | Payment CRUD + status flow |
| `payment-xendit` | Adapter Xendit (invoice + webhook) |
| `dining-api` / `dining-core` | Sesi dining, meja, guest surface |
| `customer-api` / `customer-core` | Member + registrasi |
| `employee-api` / `employee-core` | Karyawan |
| `image-api` / `image-core` / `image-imagekit` | Registry + adapter ImageKit |
| `search-api` / `search-meilisearch` | Abstraksi search + adapter Meilisearch |
| `report-api` / `report-core` | Dashboard summary (baca via contract) |
| `notification-api` / `notification-core` / `notification-resend` | Email via Resend |
| `core-app` | Entry point (`CoreAppApplication`), hanya bootstrap |

Detail dependency graph dan pola contract: [`ARCHITECTURE.md`](./ARCHITECTURE.md).

---

## Struktur Repo

```
backend/
├── core/                  # Maven multi-modul (common, *-api, *-core, core-app)
│   ├── core-app/          # Entry point + application.yml
│   └── mvnw               # Maven wrapper
├── config/
│   ├── docker.env.example # Template config Docker (SSOT)
│   └── native.env.example # Template config native
├── docker-compose.yml     # postgres + meilisearch + backend + nginx
├── reverse-proxy/nginx/   # nginx.docker.conf
├── wrapper.sh             # Helper build/run/test per modul
├── API_CONTRACT.md        # Kontrak endpoint (acuan FE)
├── AUTH.md                # Token lifecycle + matriks otorisasi
├── PROJECT_CONTEXT.md     # Snapshot konteks proyek
└── docs/                  # Diagram & flowchart
```

---

## Prasyarat

- Docker + Docker Compose (untuk cara Docker), **atau**
- Java 21 + Maven wrapper (`core/mvnw`) + PostgreSQL + Meilisearch (untuk cara native)
- GitHub PAT (`GITHUB_USER` / `GITHUB_TOKEN`) — dibutuhkan saat build Docker karena dependency `id.rascal:filter` diambil dari GitHub Packages.

---

## Quickstart

### Opsi A — Docker (disarankan)

```bash
cp config/docker.env.example config/docker.env
# isi minimal: GITHUB_USER, GITHUB_TOKEN, DATABASE_PASSWORD,
# XENDIT_*, IMAGEKIT_*, RESEND_API_KEY, MEILISEARCH_API_KEY

docker compose --env-file config/docker.env up -d --build

docker compose ps
docker compose logs -f backend
```

Akses:

| Tujuan | URL |
|---|---|
| API via proxy (disarankan) | `http://localhost:9000` |
| API langsung | `http://localhost:8081` |
| Health check | `GET http://localhost:9000/health` → `{"status":"ok"}` |

Berhenti:

```bash
docker compose down        # data volume tetap ada
docker compose down -v     # hapus juga data postgres & meili
```

> `docker-compose.yml` tidak menyimpan nilai config — semua variabel berasal dari `config/docker.env` (SSOT). Jangan edit compose untuk urusan config.

### Opsi B — Native (tanpa Docker)

```bash
cp config/native.env.example config/native.env
# sesuaikan DATABASE_*, MEILISEARCH_URL, kredensial eksternal

# pastikan Postgres & Meilisearch sudah jalan di host yang dikonfigurasi
./wrapper.sh install-all
./wrapper.sh run -- --seed dev
```

Seed (`--seed dev` = akun demo + data contoh, `--seed formal` = role/authority produksi tanpa data demo):

```bash
./wrapper.sh run -- --seed dev
./wrapper.sh run -- --seed formal
```

Perintah `wrapper.sh` lain:

```bash
./wrapper.sh                  # menu interaktif
./wrapper.sh install-one menu-core
./wrapper.sh compile order-core
./wrapper.sh test payment-core
./wrapper.sh doctor
```

> `run` memakai modul sibling dari `~/.m2` — jalankan `install-all` / `install-one` dulu setelah mengubah modul lain.

---

## Konfigurasi

Spring Boot memilih file env via `APP_ENV` (`native` / `docker`) — lihat `core/core-app/src/main/resources/application.yml`.

Variabel penting (`config/docker.env.example` / `config/native.env.example`):

| Var | Contoh | Untuk |
|---|---|---|
| `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `DATABASE_NAMES_CORE_BACKEND` | `postgres / *** / projekan` | postgres + backend |
| `SPRING_PROFILES` | `postgres` (docker) / `postgres,dev-seed` (native) | datasource profile |
| `MEILISEARCH_URL`, `MEILISEARCH_API_KEY` | `http://meilisearch:7700` (docker) / `http://localhost:7700` (native) | search |
| `BACKEND_PORT`, `NGINX_PORT`, `DB_EXTERNAL_PORT`, `MEILI_EXTERNAL_PORT` | `8081`, `9000`, `5432`, `7700` | mapping port compose |
| `XENDIT_PRIVATE_KEY`, `XENDIT_PUBLIC_KEY`, `XENDIT_CALLBACK_TOKEN` | — | payment gateway |
| `XENDIT_SUCCESS_REDIRECT_URL`, `XENDIT_FAILURE_REDIRECT_URL` | `https://.../payment/status` | redirect FE setelah bayar |
| `IMAGEKIT_URL_ENDPOINT`, `IMAGEKIT_PUBLIC_KEY`, `IMAGEKIT_PRIVATE_KEY` | — | upload gambar |
| `RESEND_API_KEY`, `RESEND_FROM_EMAIL` | — | email reset password |
| `GITHUB_USER`, `GITHUB_TOKEN` | — | build arg (GitHub Packages) |

> Jangan commit `config/docker.env` / `config/native.env` — keduanya di-ignore git.

---

## Auth & Akun Default

Login:

```bash
curl -s -X POST http://localhost:9000/api/v1/auths/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@rascal.id","password":"admin123"}'
# → data.accessToken (15 menit) + cookie refresh_token (20 hari)
```

Refresh (hanya cookie, tanpa header `Authorization`):

```bash
curl -s -X POST http://localhost:9000/api/v1/auths/refresh \
  --cookie 'refresh_token=...' -c -
```

Akun seeder dev (`--seed dev`):

| Email | Password | Role |
|---|---|---|
| `admin@rascal.id` | `admin123` | `ADMIN` |
| `kasir@rascal.id` | `kasir123` | `CASHIER` |
| `waiter@rascal.id` | `waiter123` | `WAITER` |
| `kitchen@rascal.id` | `kitchen123` | `KITCHEN` |

Error 401 dibedakan via `errorCode`: `ACCESS_TOKEN_EXPIRED` / `INVALID_ACCESS_TOKEN` → refresh dulu; `INVALID_REFRESH_TOKEN` / cookie hilang → logout → `/login`. Pola interceptor: [`API_CONTRACT.md` §4](./API_CONTRACT.md).

---

## API Overview

- Prefix: `/api/v1` (semua modul) + `/api/v2/menus` (varian cached, response ringkas).
- Content-Type: `application/json`. Auth staf: `Authorization: Bearer <accessToken>`.
- Katalog menu & surface tamu bersifat **publik** (tanpa token); sisanya butuh authority granular.

Contoh endpoint inti:

```
POST /api/v1/auths/login, /auths/refresh, /auths/logout
GET  /api/v1/menus, /api/v1/menus/{id}            # publik
GET  /api/v2/menus                                # publik, cached
POST /api/v1/orders                               # order standalone (TAKEAWAY)
GET  /api/v1/orders?status=READY&sort=createdAt,asc
POST /api/v1/orders/{id}/confirm|prepare|ready|complete|cancel
POST /api/v1/payments                             # satu-satunya jalur uang
GET  /api/v1/dinings?status=OPEN
POST /api/v1/dinings  {tableId}
POST /api/v1/dinings/{id}/orders
POST /api/v1/dinings/{id}/close
GET  /api/v1/guest/dinings/{guestToken}           # publik (polling tamu)
POST /api/v1/guest/dinings/{guestToken}/orders    # publik
GET  /api/v1/guest/orders/{trackToken}            # publik
GET  /api/v1/my/dinings, /api/v1/my/orders        # member login
GET  /api/v1/kitchen/orders                       # kitchen.read
GET  /api/v1/reports/dashboard/summary            # report.read
GET  /api/v1/images/auth                          # image.create
```

Format response standar:

```json
{ "isSuccess": true, "message": "...", "data": { }, "meta": { "timestamp": "..." } }
{ "isSuccess": false, "message": "...", "errorCode": "NOT_FOUND", "meta": { "timestamp": "..." } }
```

List memakai Spring `Pageable` (`page` 0-based, `size`, `sort`) tapi response `meta.pagination.currentPage` 1-based. Status flows (order / payment / invoice / dining) dan matriks otorisasi lengkap: [`API_CONTRACT.md`](./API_CONTRACT.md) dan [`AUTH.md`](./AUTH.md).

---

## Testing

```bash
./wrapper.sh test payment-core
./wrapper.sh test order-core --test OrderServiceCreateTest
```

Test tersebar per modul (`src/test`, contoh: `order-core`, `payment-core`, `dining-core`, `invoice-core`, wiring di `core-app`). DB test memakai H2 (`scope=test` di `core-app`).

---

## Operasional

- **Health:** `GET /health` di Nginx; healthcheck compose: `pg_isready` (postgres), `curl /health` (meilisearch), `pgrep java` (backend, `depends_on: healthy` berurutan).
- **Rate limit:** Nginx `10r/s` per IP (`burst=20`) untuk `/api/`, `429` bila lewat.
- **CORS:** ditangani proxy (Vercel di produksi / Nginx lokal) — request terlihat same-origin; `OPTIONS /**` di-`permitAll` di `SecurityConfig`.
- **Pagination default:** orders/payments/dinings `createdAt,desc`; kategori/modifier/meja ascending; `GET /menus` tanpa `sort` = ranking Meilisearch.

---

## Troubleshooting

| Gejala | Penyebab umum | Aksi |
|---|---|---|
| Build Docker gagal ambil `id.rascal:filter` | `GITHUB_TOKEN` kosong / PAT kedaluwarsa | Isi `GITHUB_USER` + `GITHUB_TOKEN` di `config/docker.env` |
| Backend `unhealthy` / restart | Postgres/Meilisearch belum healthy, env DB salah | `docker compose ps`, `docker compose logs -f backend`, cek `DATABASE_*` & `MEILISEARCH_*` |
| `401 ACCESS_TOKEN_EXPIRED` | Access token > 15 menit | `POST /auths/refresh` (cookie), ulangi request sekali |
| `401 INVALID_REFRESH_TOKEN` | Refresh dicabut / kedaluwarsa | Logout, login ulang |
| `403` padahal sudah login | Authority kurang untuk endpoint itu | Cek `AUTH.md` §4 + `authorities` di JWT |
| Menu search error / lambat | Meilisearch down / key salah | Backend fallback ke Postgres otomatis; cek `MEILISEARCH_URL` + `MEILISEARCH_API_KEY`, `docker compose logs -f meilisearch` |
| Webhook Xendit ditolak | `X-Callback-Token` tidak cocok | Samakan `XENDIT_CALLBACK_TOKEN` dengan dashboard Xendit |
| Perubahan modul tidak terbawa saat `run` | Sibling diambil dari `~/.m2` | `./wrapper.sh install-all` (atau `install-one <modul>`) dulu, baru `run` |

---

## Dokumentasi Terkait

| Dokumen | Isi |
|---|---|
| [`API_CONTRACT.md`](./API_CONTRACT.md) | Spesifikasi endpoint + contoh request/response (acuan FE) |
| [`AUTH.md`](./AUTH.md) | Token lifecycle, filter chain, matriks otorisasi |
| [`ARCHITECTURE.md`](./ARCHITECTURE.md) | Aturan modul, dependency graph, seed framework |
| [`PROJECT_CONTEXT.md`](./PROJECT_CONTEXT.md) | Snapshot konteks + status phase |
| [`DOCKER-SETUP.md`](./DOCKER-SETUP.md) | Service compose + env SSOT |
| [`SEARCH.md`](./SEARCH.md) | Read projection Meilisearch |

---

## Lisensi

Internal project — Hysteria Cafe. Tidak untuk distribusi publik tanpa izin pemilik repo.
