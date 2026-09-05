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
(`@EnableMethodSecurity`) hanya dipakai `RoleController` dan
`AuthorityController` lewat `@PreAuthorize`; anotasi otorisasi di
controller menu saat ini di-comment sehingga seluruh endpoint menu hanya
membutuhkan login.

---

## 4. Matriks Otorisasi

| Endpoint | Syarat |
|---|---|
| `POST /auths/login`, `POST /auths/refresh`, kedua webhook | Publik |
| `POST /auths/users`, `GET /auths/users`, `GET /auths/users/{id}`, `PUT/PATCH/DELETE /auths/users/{id}` | Login |
| `POST /auths/roles` | `role.create` / `role.*` |
| `GET /auths/roles`, `GET /auths/roles/{id}` | `role.read` / `role.*` |
| `PUT/PATCH /auths/roles/{id}` | `role.update` / `role.*` |
| `DELETE /auths/roles/{id}` | `role.delete` / `role.*` |
| `GET /auths/authorities` | `authority.read` / `authority.*` |
| `GET /auths/authorities/{id}` | `authority.create` / `authority.*` (quirk — bukan `read`, mengikuti anotasi di source) |
| `DELETE /auths/authorities/{id}` | `authority.delete` / `authority.*` |
| Menu (V1, V2, admin, categories, modifiers), Order, Payment, Dining, Table, `GET /images/auth` | Login |

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
