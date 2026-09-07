# Auth — Token Lifecycle dan Otorisasi

> Cara kerja login, refresh, dan hak akses backend. Contoh request/response
> per endpoint ada di `API_CONTRACT.md` (bagian Autentikasi); pola
> interceptor frontend juga di sana.

---

## 1. Komponen

| Komponen | Lokasi | Peran |
|---|---|---|
| `JwtAuthFilter` | Library eksternal `id.rascal:filter:1.1.0` | Validasi JWT tiap request, menerbitkan access token |
| `SecurityConfig` | `auth-core/.../config/SecurityConfig.java` | Filter chain: stateless, CSRF off, daftar endpoint publik, bypass JWT |
| `SecurityExceptionHandler` | `auth-core/.../exception/` | Response 401/403 dalam format API standar |
| `AuthService` / `AuthServiceImpl` | `auth-core/.../service/` | Login, refresh (dengan rotasi), logout, logout-all |
| `RefreshTokenCookieFactory` | `auth-core/.../service/` | Membuat dan menghapus cookie `refresh_token` |
| `RefreshTokenCleanupService` | `auth-core/.../service/` | Pembersihan refresh token kedaluwarsa |
| `RefreshToken` (entity + repository) | `auth-core/.../entity/`, `.../repository/` | Penyimpanan refresh token di PostgreSQL |

---

## 2. Token Lifecycle

### Access token

- JWT HMAC-SHA yang diterbitkan library filter, umur **15 menit**.
- Claims: `sub` (ID user sebagai string), `roles`, `authorities`.
- Dikirim lewat header `Authorization: Bearer <token>` dan disimpan di
  memori frontend — bukan localStorage.

### Refresh token

- String acak Base64 64 bytes, umur **20 hari** (`expiresAt = now + 20 hari`
  di `AuthServiceImpl`, `maxAge = Duration.ofDays(20)` di cookie factory).
- Disimpan dua tempat: record `RefreshToken` di database dan cookie
  `refresh_token` (`httpOnly=true`, `secure=false`, `sameSite=Strict`,
  `path=/api/v1/auths`).

### Rotasi saat refresh

Setiap `POST /auths/refresh` yang sukses tidak hanya mengembalikan access
token baru — refresh token ikut **dirotasi**: token lama dicabut dan cookie
baru ter-set lewat `Set-Cookie` di response. Browser cukup membiarkan
cookie tertimpa; tidak ada aksi khusus dari frontend. Akibatnya, logout
dari satu perangkat tidak mengganggu perangkat lain kecuali lewat
`logout-all` yang mencabut seluruh token milik user.

### Alur hidup sesi

```
login ──► access (15 mnt) + refresh (20 hari) tersimpan
   ├── tiap 15 menit: refresh ──► access baru + refresh baru (rotasi)
   ├── logout: refresh di cookie dicabut + cookie dihapus
   ├── logout-all: semua refresh milik user dicabut + cookie dihapus
   └── refresh kedaluwarsa/dicabut: 401 INVALID_REFRESH_TOKEN → login ulang
```

---

## 3. Filter Chain dan Endpoint Publik

`SecurityConfig` memakai sesi stateless dengan CSRF dimatikan dan
`OPTIONS /**` selalu diizinkan (preflight CORS). `jwtBypassFilter`
melewatkan `JwtAuthFilter` untuk tiga path berikut, yang juga terdaftar
sebagai `permitAll`:

| Path | Sifat |
|---|---|
| `/api/v1/auths/login` | Publik |
| `/api/v1/auths/refresh` | Publik, hanya lewat cookie |
| `/api/v1/payments/webhooks/xendit` | Publik, server-to-server (`X-Callback-Token`) |
| `/api/v1/images/imagekit/webhooks` | Publik, server-to-server |

Semua request lain wajib `authenticated()`. Method security
(`@EnableMethodSecurity`) aktif dan `@PreAuthorize` granular dipasang di
seluruh controller resource (user, role, authority, menu, order, payment,
dining, table, image, report, customer) sesuai matriks §4. Wildcard literal
(`menu.*`, `order.*`, dst.) ikut dicek karena ADMIN memegang string
tersebut sebagai authority; role non-admin lolos lewat permission
granular. Khusus `report.read` tidak ada wildcard `report.*` di catalog,
sehingga gate-nya hanya authority tunggal itu (ADMIN & CASHIER).

---

## 4. Matriks Otorisasi

| Endpoint | Syarat |
|---|---|
| `POST /auths/login`, `POST /auths/refresh`, kedua webhook, `POST /customers/register` | Publik |
| `POST /auths/users` | `user.create` / `user.*` |
| `GET /auths/users`, `GET /auths/users/{id}` | `user.read` / `user.*` |
| `PUT/PATCH /auths/users/{id}` | `user.update` / `user.*` |
| `DELETE /auths/users/{id}` | `user.delete` / `user.*` |
| `POST /auths/roles` | `role.create` / `role.*` |
| `GET /auths/roles`, `GET /auths/roles/{id}` | `role.read` / `role.*` |
| `PUT/PATCH /auths/roles/{id}` | `role.update` / `role.*` |
| `DELETE /auths/roles/{id}` | `role.delete` / `role.*` |
| `GET /auths/authorities` | `authority.read` / `authority.*` |
| `GET /auths/authorities/{id}` | `authority.create` / `authority.*` (quirk — bukan `read`, mengikuti anotasi di source) |
| `DELETE /auths/authorities/{id}` | `authority.delete` / `authority.*` |
| Menu V1/V2: `POST /` | `menu.create` / `menu.*` |
| Menu V1: `GET /`, `GET /{id}` | Public — permitAll di SecurityConfig, tanpa auth |
| Menu V2: `GET /`, `GET /{id}` | `menu.read` / `menu.*` |
| Menu V1/V2: `PUT /{id}`, `PATCH /{id}/restore` | `menu.update` / `menu.*` |
| Menu V1/V2: `DELETE /{id}` | `menu.delete` / `menu.*` |
| Admin menu: `GET /search`, `GET /{id}` | `menu.read` / `menu.*` |
| Kategori menu: `POST /` | `menu-category.create` / `menu-category.*` |
| Kategori menu: `GET /`, `GET /{id}` | `menu-category.read` / `menu-category.*` |
| Kategori menu: `PUT /{id}`, `PATCH /{id}/restore` | `menu-category.update` / `menu-category.*` |
| Kategori menu: `DELETE /{id}` | `menu-category.delete` / `menu-category.*` |
| Modifier: `POST /` | `menu-modifier.create` / `menu-modifier.*` |
| Modifier: `GET /`, `GET /{id}` | `menu-modifier.read` / `menu-modifier.*` |
| Modifier: `PUT /{id}` | `menu-modifier.update` / `menu-modifier.*` |
| Modifier: `DELETE /{id}` | `menu-modifier.delete` / `menu-modifier.*` |
| Order: `POST /` | `order.create` / `order.*` |
| Order: `GET /`, `GET /{id}` | `order.read` / `order.*` |
| Order: `PUT /{id}`, `PATCH /{id}`, `POST /{id}/confirm`, `POST /{id}/cancel` | `order.update` / `order.*` |
| Order: `POST /{id}/prepare` | `order.mark.preparing` / `order.*` |
| Order: `POST /{id}/ready` | `order.mark.ready` / `order.*` |
| Order: `POST /{id}/complete` | `order.mark.completed` / `order.*` |
| Order: `DELETE /{id}` | `order.delete` / `order.*` |
| Payment: `POST /` | `payment.create` / `payment.*` |
| Payment: `GET /`, `GET /{id}` | `payment.read` / `payment.*` |
| Payment: `POST /{id}/expire` / `/fail` / `/refund` | `payment.update` / `payment.*` |
| Dining: `POST /` | `dining.create` / `dining.*` |
| Dining: `GET /`, `GET /{id}` | `dining.read` / `dining.*` |
| Dining: `POST /{id}/orders`, `POST /{id}/close` | `dining.update` / `dining.*` |
| Table: `POST /` | `table.create` / `table.*` |
| Table: `GET /`, `GET /{id}` | `table.read` / `table.*` |
| Table: `PUT /{id}`, `PATCH /{id}` | `table.update` / `table.*` |
| Table: `DELETE /{id}` | `table.delete` / `table.*` |
| `GET /images/auth` | `image.create` / `image.*` |
| `GET /reports/dashboard/summary` | `report.read` (tanpa wildcard — hanya ADMIN & CASHIER) |
| Customer: `POST /customers/register` | Publik (buat akun `auth_users` + profil member) |
| Customer: `POST /` | `customer.create` / `customer.*` |
| Customer: `GET /`, `GET /{id}` | `customer.read` / `customer.*` |
| Customer: `PUT/PATCH /{id}`, `POST /{id}/claim` | `customer.update` / `customer.*` |
| Customer: `DELETE /{id}` | `customer.delete` / `customer.*` |

### 4.1 Matriks Role × Authority (sumber kebenaran seeder)

Role final: **ADMIN, CASHIER, WAITER, KITCHEN + CUSTOMER_BASE**.
`CUSTOMER_BASE` adalah pengecualian eksplisit atas larangan role `CUSTOMER`:
murni identitas login customer (nol authority staf → otomatis 403 di semua
endpoint staf), bukan guard untuk memaksa login sebelum pesan — guest
checkout tetap. Tidak ada `OWNER` (dobel konsep dengan ADMIN). Berlaku
untuk `DevRoleSeeder` dan `FormalRoleSeeder`; seluruh authority dijamin
ada di `AuthorityCatalog`.

| Group | ADMIN | CASHIER | WAITER | KITCHEN |
|---|---|---|---|---|
| user / role / authority | semua | — | — | — |
| menu / category / modifier / image | semua | read | read | read |
| order CRUD (tanpa delete agresif) | semua | create/read/update | create/read/update | read |
| `order.mark.preparing` / `order.mark.ready` | ✓ | — | — | ✓ |
| `order.mark.completed` | ✓ | ✓ | ✓ | — |
| payment create/read/update | ✓ | ✓ | read saja | — |
| `payment.resolve` | ✓ | — | — | — |
| dining / table | semua | read | create/read/update | — |
| `kitchen.read` / `kitchen.update` | ✓ | — | — | ✓ |
| `report.read` | ✓ | ✓ | — | — |

Catatan penyesuaian saat implementasi (matriks inti roadmap disesuaikan):

- WAITER mendapat `payment.read` (read-only, pilihan "— atau read saja");
  tidak mendapat `order.mark.preparing/ready` (opsional, tidak di-assign).
- CASHIER mendapat `dining.read` + `table.read` (read-only);
  `customer.create/read/update` (kelola member + claim akun); WAITER
  mendapat `customer.read` (cari member).
- Assignment granular (tanpa wildcard) kecuali ADMIN yang memegang semua
  authority termasuk `x.*` untuk keperluan assign/UI.

---

## 5. Error 401 — Panduan Keputusan

| errorCode | Arti | Aksi frontend |
|---|---|---|
| `ACCESS_TOKEN_EXPIRED` | JWT melewati 15 menit | Refresh (errorCode dari library filter — verifikasi saat runtime) |
| `INVALID_ACCESS_TOKEN` | JWT rusak atau signature tidak cocok | Refresh |
| `INVALID_REFRESH_TOKEN` | Refresh token tidak valid, kedaluwarsa, atau dicabut | Logout, redirect `/login` |
| `UNAUTHORIZED` + `"Refresh token is missing"` | Cookie tidak ada | Logout, redirect `/login` |
| `UNAUTHORIZED` + pesan lain | Bukan masalah token (mis. hak akses) | Tampilkan error tanpa redirect |

Aturan refresh: panggil `POST /api/v1/auths/refresh` tanpa header
`Authorization` dengan `withCredentials: true`, simpan access token baru,
ulangi request yang gagal exactly once (flag `_retry`), dan gabungkan
request paralel yang 401 bersamaan agar hanya satu refresh berjalan.
