# API Contract — Backend Modular Monolith

Dokumen ini berisi spesifikasi API lengkap untuk integrasi Frontend. Semua informasi
di bawah verified langsung dari source code per **30 Agustus 2026**.

---

## 1. Informasi Dasar

| Item | Nilai |
|---|---|
| Base URL | `http://localhost:8081` |
| API Prefix | `/api/v1` (semua modul) & `/api/v2` (Menu cached) |
| Content-Type | `application/json` untuk semua request & response body |
| Port Proxy (dev) | `9000` — Nginx merutekan `/api/` → backend:8081, `/` → frontend:5173 |
| CORS | Ditangani di Nginx level proxy — frontend bebas request ke port 9000 |

---

## 2. Konvensi Pagination

Semua list endpoint menggunakan Spring Data `Pageable`:

| Parameter | Tipe | Default | Keterangan |
|---|---|---|---|
| `page` | int | `0` | **0-based** index (halaman pertama = 0) |
| `size` | int | `10` | Jumlah data per halaman |
| `sort` | string | bervariasi | Format `namaField,arah` — contoh: `createdAt,desc` |

**Response** menggunakan format **1-based** di field `meta.pagination.currentPage`.
Artinya: request `page=0` → response `currentPage: 1`.

---

## 3. Format Response Standar

### A. Success (single)
```json
{
  "isSuccess": true,
  "message": "Data retrieved",
  "data": { ... },
  "meta": { "timestamp": "2026-08-30T10:00:00Z" }
}
```

### B. Success (paged)
```json
{
  "isSuccess": true,
  "message": "Data retrieved",
  "data": [ ... ],
  "meta": {
    "pagination": {
      "currentPage": 1,
      "perPage": 10,
      "totalItems": 42,
      "totalPages": 5,
      "hasNextPage": true,
      "hasPrevPage": false
    },
    "timestamp": "2026-08-30T10:00:00Z"
  }
}
```

### C. Error
```json
{
  "isSuccess": false,
  "message": "User not found",
  "errorCode": "NOT_FOUND",
  "errors": null,
  "meta": { "timestamp": "2026-08-30T10:00:00Z" }
}
```

### D. Validation Error
```json
{
  "isSuccess": false,
  "message": "Validation failed",
  "errorCode": null,
  "errors": [
    { "field": "email", "message": "Email is required" },
    { "field": "password", "message": "Password must be at least 8 characters" }
  ],
  "meta": { "timestamp": "2026-08-30T10:00:00Z" }
}
```

---

## 4. Autentikasi & Token Management

### Mekanisme Token

| Token | Tipe | Umur | Storage | Cara Kirim |
|---|---|---|---|---|
| **Access Token** | JWT (HMAC-SHA) | **15 menit** | Variable di memori (bukan localStorage) | Header: `Authorization: Bearer <token>` |
| **Refresh Token** | Random Base64 (64 bytes) | **20 hari** | Cookie `httpOnly` | Otomatis dikirim browser (path: `/api/v1/auths`) |

### Claims Access Token
```json
{
  "sub": "1",                    // ID user (string)
  "roles": ["ADMIN"],            // array nama role
  "authorities": ["menu.create", "order.read"]  // array nama authority
}
```

### Cookie Refresh Token — Konfigurasi

| Property | Nilai |
|---|---|
| Nama cookie | `refresh_token` |
| `httpOnly` | `true` — tidak bisa diakses dari JS |
| `secure` | `false` (dev) / `true` (prod) |
| `sameSite` | `Strict` |
| `path` | `/api/v1/auths` — cookie **hanya** dikirim ke path ini |
| `maxAge` | 20 hari |

> **Penting:** Karena `path` = `/api/v1/auths`, cookie `refresh_token` **hanya** dikirim oleh browser
> ke endpoint yang diawali `/api/v1/auths/`. Endpoint lain (menus, orders, dll) **tidak** akan
> menerima cookie ini. Ini keamanan — tidak ada refresh token yang bocor ke endpoint non-auth.

---

### ErrorCode dari JwtAuthFilter — DASAR KEPUTUSAN FRONTEND

Library security (`JwtAuthFilter`) menghasilkan **3 jenis errorCode** yang berbeda
untuk setiap kondisi. Frontend **wajib** membedakan ketiganya:

| errorCode | HTTP | message | Arti | Aksi FE |
|---|---|---|---|---|
| `ACCESS_TOKEN_EXPIRED` | 401 | `"Access token expired"` | JWT sudah lewat waktu 15 menit | 🔄 **Coba refresh** — panggil `POST /auths/refresh` |
| `INVALID_ACCESS_TOKEN` | 401 | `"Invalid access token"` | JWT rusak / signature tidak cocok / format salah | 🔄 **Coba refresh** — mungkin token corrupt, refresh bisa berikan yang baru |
| `UNAUTHORIZED` | 401 | *(beragam)* | Tidak ada header, atau header bukan Bearer, atau role tidak cukup, atau refresh token bermasalah | ⚠️ **Cek errorCode spesifik** — lihat bawah |

---

### Alur Lengkap Keputusan Frontend saat Terima 401

```
Response 401 dari backend
│
├── errorCode == "ACCESS_TOKEN_EXPIRED"
│   │
│   └── 🔄 REFRESH: panggil POST /api/v1/auths/refresh
│       │         (JANGAN kirim header Authorization, cukup cookie otomatis)
│       │
│       ├── 200 OK → dapat access token baru
│       │   ├── simpan token baru di variable
│       │   └── ULANGI request yang gagal tadi
│       │
│       └── 401 dengan errorCode "INVALID_REFRESH_TOKEN"
│           └── 🔴 REVOKE & REDIRECT: refresh token expired/revoke
│               ├── (cookie sudah otomatis di-clear oleh backend)
│               ├── clear access token dari memori
│               └── redirect ke /login
│
├── errorCode == "INVALID_ACCESS_TOKEN"
│   │
│   └── 🔄 REFRESH: coba refresh juga (token mungkin corrupt)
│       ├── 200 OK → simpan token baru, ulangi request
│       └── 401 "INVALID_REFRESH_TOKEN" → 🔴 redirect ke /login
│
├── errorCode == "INVALID_REFRESH_TOKEN"
│   │
│   └── 🔴 REVOKE & REDIRECT: refresh token expired/revoke/missing
│       ├── (cookie sudah otomatis di-clear oleh backend)
│       ├── clear access token dari memori
│       └── redirect ke /login
│
└── errorCode == "UNAUTHORIZED" (tanpa sub-spesifikasi lain)
    │
    ├── message == "Refresh token is missing"
    │   └── 🔴 REVOKE & REDIRECT: cookie refresh_token tidak ada
    │       └── redirect ke /login
    │
    └── message == lainnya ("Invalid role", "Access Denied", dll)
        └── 🔴 Tampilkan error: user tidak punya hak akses
            (jangan redirect, cukup tampilkan pesan)
```

---

### Kenapa "Coba Refresh" Bukan Langsung Logout?

Ketika backend return `ACCESS_TOKEN_EXPIRED` atau `INVALID_ACCESS_TOKEN`,
refresh token di cookie **masih VALID** (umur 20 hari). Jadi frontend masih
bisa memperpanjang sesi tanpa perlu login ulang. Hanya jika refresh endpoint
juga gagal (`INVALID_REFRESH_TOKEN`), barulah sesi benar-benar habis.

---

### Implementasi: Axios Interceptor Pattern

```javascript
import axios from 'axios';

const api = axios.create({ baseURL: '/api/v1' });

// State untuk cegah refresh loop
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    error ? reject(error) : resolve(token);
  });
  failedQueue = [];
};

// ── REQUEST INTERCEPTOR ──
api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

// ── RESPONSE INTERCEPTOR ──
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // Hanya handle 401, dan hanya retry sekali
    if (error.response?.status !== 401 || originalRequest._retry) {
      return Promise.reject(error);
    }

    const errorCode = error.response.data?.errorCode;

    // ── CASE 1: Refresh token bermasalah → langsung logout ──
    if (errorCode === 'INVALID_REFRESH_TOKEN') {
      accessToken = null;
      window.location.href = '/login';
      return Promise.reject(error);
    }

    // ── CASE 2: Access token expired/invalid → coba refresh ──
    if (errorCode === 'ACCESS_TOKEN_EXPIRED' || errorCode === 'INVALID_ACCESS_TOKEN') {
      
      // Kalau sudah sedang refresh, queue request ini
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return api(originalRequest);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // ── PANGGIL REFRESH (tanpa Authorization header!) ──
        const { data } = await axios.post('/api/v1/auths/refresh', null, {
          withCredentials: true,  // pastikan cookie dikirim
        });

        const newToken = data.data.accessToken;
        accessToken = newToken;

        processQueue(null, newToken);

        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return api(originalRequest);

      } catch (refreshError) {
        processQueue(refreshError, null);
        accessToken = null;
        window.location.href = '/login';
        return Promise.reject(refreshError);

      } finally {
        isRefreshing = false;
      }
    }

    // ── CASE 3: UNAUTHORIZED lainnya → reject biasa ──
    return Promise.reject(error);
  }
);
```

**Poin penting dari kode di atas:**
1. `POST /api/v1/auths/refresh` dipanggil **TANPA** header `Authorization` — hanya cookie
2. `withCredentials: true` wajib agar browser mengirim cookie lintas origin
3. `isRefreshing` + `failedQueue` mencegah 10 request paralel masing-masing trigger refresh
4. `originalRequest._retry = true` mencegah infinite loop jika refresh berhasil tapi request tetap 401
5. Jika refresh gagal (`INVALID_REFRESH_TOKEN`), langsung redirect ke login

---

### Ringkasan: Kapan Refresh vs Kapan Logout

| Kondisi | Aksi | Penjelasan |
|---|---|---|
| `ACCESS_TOKEN_EXPIRED` | 🔄 **Refresh** | Token expired, tapi refresh token masih hidup (20 hari) |
| `INVALID_ACCESS_TOKEN` | 🔄 **Refresh** | Token corrupt, coba refresh dulu |
| `INVALID_REFRESH_TOKEN` | 🔴 **Logout** | Refresh token expired/revoke — tidak bisa perpanjang sesi |
| `UNAUTHORIZED` + "Refresh token is missing" | 🔴 **Logout** | Cookie tidak ada — user belum login atau sudah logout |
| `UNAUTHORIZED` + pesan lain (role, akses) | ⚠️ **Tampilkan error** | Bukan masalah token, tapi hak akses |

---

### Masa Aktif Token

```
Access Token:  ████████████████░░░░░░░░░░░░░░░░  15 menit
Refresh Token: ████████████████████████████████  20 hari

Login ──────────────────────────────────────────────────── 20 hari
  │
  ├── request pertama: pakai access token
  ├── 15 menit: access token expired → refresh → token baru
  ├── 30 menit: expired lagi → refresh → token baru
  ├── ... (berulang selama refresh token masih hidup)
  └── 20 hari: refresh token expired → refresh gagal → redirect /login
```

---

## 5. Matriks Otorisasi

| Endpoint | Auth Required | Authority |
|---|---|---|
| `POST /auths/login` | Tidak | — |
| `POST /auths/register` | Tidak | — |
| `POST /auths/refresh` | Tidak (cookie only) | — |
| `POST /auths/logout` | Ya (cookie) | — |
| `POST /auths/logout-all` | Ya (Bearer) | — |
| `CRUD /auths/users` | Ya | Cukup login |
| `GET/DELETE /auths/authorities` | Ya | `authority.read` / `authority.delete` / `authority.*` |
| `GET /auths/authorities/{id}` | Ya | `authority.create` / `authority.*` ⚠️ |
| `CRUD /auths/roles` | Ya | `role.create` / `role.read` / `role.update` / `role.delete` / `role.*` |
| Semua endpoint Menu, Order, Payment, Dining, Table | Ya | Cukup login |

> ⚠️ **Quirk:** `GET /auths/authorities/{id}` membutuhkan authority `authority.create` (bukan `read`).
> Ini karena implementasi backend saat ini menggunakan anotasi `authority.create` untuk GetById.

---

## 6. Endpoint Lengkap per Modul

---

### A. Auth (`/api/v1/auths`)

#### Login
```
POST /api/v1/auths/login
```
**Request:**
```json
{
  "email": "admin@rascal.id",
  "password": "admin123"
}
```
**Response 200:**
```json
{
  "isSuccess": true,
  "message": "Login success, welcome back!",
  "data": {
    "id": 1,
    "email": "admin@rascal.id",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
  },
  "meta": { "timestamp": "..." }
}
```
**Dampak:** Cookie `refresh_token` otomatis di-set oleh server (httpOnly, sameSite=Strict, path=/api/v1/auths).

#### Refresh Token
```
POST /api/v1/auths/refresh
```
**Request:** Tidak perlu body. Cukup cookie `refresh_token` otomatis dikirim browser.
**Response 200:**
```json
{
  "isSuccess": true,
  "message": "Request processed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9..."   // access token baru
  },
  "meta": { "timestamp": "..." }
}
```
> **Catatan:** Endpoint ini **publik** — `JwtAuthFilter` dibypass. Tidak perlu kirim header `Authorization`.

#### Logout
```
POST /api/v1/auths/logout
```
**Request:** Cookie `refresh_token` otomatis dikirim.
**Response 200:**
```json
{
  "isSuccess": true,
  "message": "Logout success",
  "data": null,
  "meta": { "timestamp": "..." }
}
```
**Dampak:** Cookie `refresh_token` dihapus (set maxAge=0).

#### Logout All Devices
```
POST /api/v1/auths/logout-all
```
**Auth:** Bearer token (header `Authorization`).
**Response 200:**
```json
{
  "isSuccess": true,
  "message": "Logout all success",
  "data": null,
  "meta": { "timestamp": "..." }
}
```
**Dampak:** Semua refresh token user dihapus dari DB + cookie lokal dihapus.

---

### B. Users (`/api/v1/auths/users`)

| Method | Path | Keterangan |
|---|---|---|
| `POST /` | Create user | Butuh `roleIds` |
| `GET /` | List users | Filter: `email` (substring), pagination |
| `GET /{id}` | Get user by ID | |
| `PUT /{id}` | Full update | |
| `PATCH /{id}` | Partial update | Field: `email`, `password`, `roleIds` (opsional) |
| `DELETE /{id}` | Soft delete | |

**Create User Request:**
```json
{
  "email": "user@rascal.id",
  "password": "securepassword123",
  "roleIds": [1]
}
```

**UserAuthResponse:**
```json
{
  "id": 1,
  "email": "user@rascal.id",
  "roles": [
    {
      "id": 1,
      "name": "ADMIN",
      "authorities": [
        { "id": 1, "name": "menu.create", "createdAt": "...", "updatedAt": null }
      ],
      "createdAt": "...",
      "updatedAt": null
    }
  ],
  "createdAt": "2026-08-23T00:00:00Z",
  "updatedAt": null
}
```

---

### C. Roles (`/api/v1/auths/roles`)

| Method | Path | Authority |
|---|---|---|
| `POST /` | Create | `role.create` / `role.*` |
| `GET /` | List | `role.read` / `role.*` |
| `GET /{id}` | Get by ID | `role.read` / `role.*` |
| `PUT /{id}` | Full update | `role.update` / `role.*` |
| `PATCH /{id}` | Partial update | `role.update` / `role.*` |
| `DELETE /{id}` | Soft delete | `role.delete` / `role.*` |

**Create Role Request:**
```json
{
  "name": "CASHIER",
  "authorityIds": [1, 2, 3]
}
```

**RoleResponse:**
```json
{
  "id": 1,
  "name": "ADMIN",
  "authorities": [
    { "id": 1, "name": "menu.create", "createdAt": "...", "updatedAt": null }
  ],
  "createdAt": "...",
  "updatedAt": null
}
```

---

### D. Authorities (`/api/v1/auths/authorities`)

| Method | Path | Authority |
|---|---|---|
| `GET /` | List | `authority.read` / `authority.*` |
| `GET /{id}` | Get by ID | ⚠️ `authority.create` / `authority.*` |
| `DELETE /{id}` | Delete | `authority.delete` / `authority.*` |

**AuthorityResponse:**
```json
{
  "id": 1,
  "name": "menu.create",
  "createdAt": "2026-08-23T00:00:00Z",
  "updatedAt": null
}
```

---

### E. Menus V1 (`/api/v1/menus`)

| Method | Path | Keterangan |
|---|---|---|
| `POST /` | Create menu | |
| `GET /` | List menus | Filter: `name` (substring), `categoryId`, `minPrice`, `maxPrice` |
| `GET /{id}` | Get by ID | |
| `PUT /{id}` | Full update | |
| `PATCH /{id}/restore` | Restore soft-deleted | |
| `DELETE /{id}` | Soft delete | Response: `204 No Content` |

**Admin read endpoints (`/api/v1/admin/menus`):**

| Method | Path | Keterangan |
|---|---|---|
| `GET /search` | Search menu | Filter: `name`, `categoryId`, `minPrice`, `maxPrice`, `isAvailable`, `deleted=active\|deleted\|all` (default `active`) |
| `GET /{id}` | Get by ID | Termasuk menu soft-deleted; `deletedAt` terisi bila deleted |

> ℹ️ **Baca (V1 & admin) di-backend oleh Meilisearch (read projection).** Saat Meilisearch down, search & detail otomatis fallback ke PostgreSQL. Tanpa param `sort`, hasil diurutkan oleh relevance ranking Meilisearch — untuk urutan deterministik kirim `sort=name|basePrice|createdAt` (+ `asc`/`desc`). Endpoint customer **selalu** memfilter `isDeleted=false` di sisi server; menu soft-deleted tidak pernah muncul.

**Create/Update Menu Request:**
```json
{
  "name": "Kopi Latte",
  "categoryIds": [1],
  "description": "Kopi dengan susu segar",
  "imageUrls": ["https://image.url/latte.png"],
  "basePrice": 25000,
  "isAvailable": true,
  "ModifierTypeIds": [1]
}
```
> ⚠️ **Catatan:** Field `ModifierTypeIds` menggunakan huruf besar `M` — ini sesuai source code.

**MenuResponse (V1):**
```json
{
  "id": 1,
  "name": "Kopi Latte",
  "description": "Kopi dengan susu segar",
  "categories": [
    { "id": 1, "name": "Kopi", "categoryCode": "COFFEE", "displayOrder": 1 }
  ],
  "imageUrls": ["https://image.url/latte.png"],
  "basePrice": 25000,
  "isAvailable": true,
  "createdAt": "2026-08-23T00:00:00Z",
  "updatedAt": null,
  "modifierTypes": [
    {
      "id": 1,
      "name": "Sugar Level",
      "minSelection": 1,
      "maxSelection": 1,
      "options": [
        { "id": 1, "name": "Normal", "additionalPrice": 0 },
        { "id": 2, "name": "Extra", "additionalPrice": 2000 }
      ]
    }
  ],
  "deletedAt": null
}
```
> `deletedAt` (nullable) — baru (additive). Selalu `null` di response customer; admin (`/api/v1/admin/menus/{id}`) mengisinya saat menu di-soft-delete. FE: `deletedAt: string | null`.

---

### F. Menus V2 — Cached (`/api/v2/menus`)

Path, method, request, dan logic **identik** dengan V1. Yang berbeda hanya format response —
diperkecil untuk efisiensi cache di frontend.

**MenuResponseCached:**
```json
{
  "id": 1,
  "categoryIds": [1],
  "modifierTypesIds": [1]
}
```
> Frontend bisa map `categoryIds` → category data dari cache lokal.

---

### G. Menu Categories (`/api/v1/menus/categories`)

| Method | Path | Keterangan |
|---|---|---|
| `POST /` | Create | |
| `GET /` | List | Filter: `name` (substring) |
| `GET /{id}` | Get by ID | |
| `PUT /{id}` | Full update | |
| `PATCH /{id}/restore` | Restore | |
| `DELETE /{id}` | Soft delete | Response: `204 No Content` |

**Create/Update Request:**
```json
{
  "displayName": "Hot Drinks",
  "categoryCode": "hot-drinks",
  "displayOrder": 1
}
```
> `categoryCode` harus format slug: huruf kecil + strip, contoh `hot-drinks`, `makanan-utama`.

**MenuCategoryResponse:**
```json
{
  "id": 1,
  "name": "Kopi",
  "categoryCode": "COFFEE",
  "displayOrder": 1
}
```

---

### H. Modifiers (`/api/v1/menus/modifiers`)

| Method | Path | Keterangan |
|---|---|---|
| `POST /` | Create modifier type + options | |
| `GET /` | List | Filter: `name` (substring) |
| `GET /{id}` | Get by ID | |
| `PUT /{id}` | Full update (termasuk options) | |
| `DELETE /{id}` | Hard delete | Response: `204 No Content` |

**Create/Update Request:**
```json
{
  "name": "Sugar Level",
  "minSelection": 1,
  "maxSelection": 1,
  "options": [
    { "name": "Normal", "additionalPrice": 0 },
    { "name": "Extra", "additionalPrice": 2000 }
  ]
}
```

**ModifierTypeResponse:**
```json
{
  "id": 1,
  "name": "Sugar Level",
  "minSelection": 1,
  "maxSelection": 1,
  "options": [
    { "id": 1, "name": "Normal", "additionalPrice": 0 },
    { "id": 2, "name": "Extra", "additionalPrice": 2000 }
  ]
}
```

---

### I. Image Upload Auth (`/api/v1/images/auth`)

Frontend upload gambar **langsung** ke ImageKit (client-side upload).
Backend hanya menyediakan kredensial bertanda-tangan.

```
GET /api/v1/images/auth
```
**Response 200:**
```json
{
  "isSuccess": true,
  "message": "Upload credentials successfully generated",
  "data": {
    "publicKey": "public_xxxx",
    "token": "random_token_16char",
    "expire": 1787163445,
    "signature": "hmac_sha1_signature"
  },
  "meta": { "timestamp": "..." }
}
```

**Flow upload gambar:**
1. Frontend minta credentials ke `GET /api/v1/images/auth`
2. Frontend upload langsung ke ImageKit menggunakan `publicKey`, `token`, `expire`, `signature`
3. ImageKit mengembalikan URL gambar
4. Frontend simpan URL tersebut di field `imageUrls` saat create/update menu
5. Backend hanya menyimpan relative path (bukan URL lengkap)

---

### J. Orders (`/api/v1/orders`)

#### Status Flow Order

```
[CREATED] ──confirm──> [CONFIRMED] ──prepare──> [PREPARING] ──ready──> [READY] ──complete──> [COMPLETED]
     │                                                                        │
  cancel                                                                     cancel
     │                                                                        │
     v                                                                        v
[CANCELLED]                                                              [CANCELLED]
```

**Aturan transisi:**
| Dari | Ke | Syarat |
|---|---|---|
| `CREATED` | `CONFIRMED` | TAKEAWAY harus `paidStatus=PAID` dulu |
| `CREATED` | `CANCELLED` | Bebas |
| `CONFIRMED` | `PREPARING` | — |
| `PREPARING` | `READY` | — |
| `READY` | `COMPLETED` | `paidStatus` harus `PAID` |
| `COMPLETED` / `CANCELLED` | — | ❌ **Terminal** — tidak bisa diubah |

#### CRUD Endpoints

| Method | Path | Keterangan |
|---|---|---|
| `POST /` | Create order | |
| `GET /` | List orders | Filter: `keyword` (orderNumber/customerName), `status`, `paidStatus` |
| `GET /{id}` | Get by ID | |
| `PUT /{id}` | Full update (reconcile items) | |
| `PATCH /{id}` | Partial update (reconcile items) | |
| `DELETE /{id}` | Hard delete | Response: `204 No Content` |

#### Status Transition Endpoints

| Method | Path | Syarat |
|---|---|---|
| `POST /{id}/confirm` | Confirm order | TAKEAWAY harus bayar dulu |
| `POST /{id}/prepare` | Mulai proses | Status harus CONFIRMED |
| `POST /{id}/ready` | Siap sajikan | Status harus PREPARING |
| `POST /{id}/complete` | Selesai | Status harus READY + sudah PAID |
| `POST /{id}/cancel` | Batal | Status harus CREATED atau CONFIRMED |

#### Create Order Request
```json
{
  "type": "DINE_IN",
  "customerId": 1,
  "customerName": "John Doe",
  "notes": "Pedas sedang",
  "items": [
    {
      "menuId": 1,
      "quantity": 2,
      "modifiers": [
        { "modifierOptionId": 1 },
        { "modifierOptionId": 5 }
      ]
    }
  ]
}
```

> **Field `type` wajib** — enum: `DINE_IN` atau `TAKEAWAY`.

#### Update Order (PUT/PATCH) — Reconcile Pattern
Saat update items, gunakan `id` untuk identify baris:
- `id` ada → update baris tersebut
- `id` null → buat baris baru
- Baris yang tidak dikirim → **dihapus** (orphan removal)

```json
{
  "items": [
    { "id": 1, "menuId": 1, "quantity": 3, "modifiers": [{ "id": 1, "modifierOptionId": 2 }] },
    { "menuId": 2, "quantity": 1, "modifiers": [] }
  ]
}
```
> Baris `id:1` di-update. Baris baru (menuId:2) ditambahkan. Baris lama yang tidak disebut dihapus.

#### OrderResponse
```json
{
  "id": 1,
  "orderNumber": "ORD-20260823-0001",
  "status": "CREATED",
  "type": "DINE_IN",
  "paidStatus": "UNPAID",
  "customerId": 1,
  "customerName": "John Doe",
  "notes": "Pedas sedang",
  "totalPrice": 54000,
  "createdAt": "2026-08-23T14:30:00Z",
  "updatedAt": null,
  "items": [
    {
      "id": 1,
      "menuId": 1,
      "itemName": "Kopi Latte",
      "unitPrice": 25000,
      "quantity": 2,
      "subtotal": 50000,
      "modifiers": [
        {
          "id": 1,
          "modifierTypeId": 1,
          "modifierOptionId": 1,
          "modifierName": "Normal",
          "additionalPrice": 0
        }
      ]
    }
  ]
}
```

**Enum values:**
| Field | Nilai |
|---|---|
| `status` | `CREATED`, `CONFIRMED`, `PREPARING`, `READY`, `COMPLETED`, `CANCELLED` |
| `type` | `DINE_IN`, `TAKEAWAY` |
| `paidStatus` | `UNPAID`, `PAID` |

---

### K. Payments (`/api/v1/payments`)

#### Status Flow Payment

```
[PENDING] ──pay──> [PAID]
   │
   ├──expire──> [EXPIRED]  (terminal)
   │
   └──fail──> [FAILED]     (terminal)

[PAID] ──refund──> [REFUNDED]  (terminal)
```

| Dari | Ke | Syarat |
|---|---|---|
| `PENDING` | `PAID` | — |
| `PENDING` | `EXPIRED` | — |
| `PENDING` | `FAILED` | — |
| `PAID` | `REFUNDED` | — |
| `EXPIRED` / `FAILED` / `REFUNDED` | — | ❌ **Terminal** — tidak bisa diubah |

#### CRUD Endpoints

| Method | Path | Keterangan |
|---|---|---|
| `POST /` | Create payment | |
| `GET /` | List payments | Filter: `keyword`, `targetType`, `targetId`, `status`, `paymentMethodId` |
| `GET /{id}` | Get by ID | |
| `PUT /{id}` | Full update | |
| `PATCH /{id}` | Partial update | |
| `DELETE /{id}` | Hard delete | Response: `204 No Content` |

#### Status Transition Endpoints

| Method | Path |
|---|---|
| `POST /{id}/pay` | Mark as PAID |
| `POST /{id}/expire` | Mark as EXPIRED |
| `POST /{id}/fail` | Mark as FAILED |
| `POST /{id}/refund` | Mark as REFUNDED |

#### Create Payment Request
```json
{
  "targetType": "ORDER",
  "targetId": 1,
  "paymentMethodId": 1,
  "paymentChannel": "XENDIT",
  "paymentDetail": "BCA Virtual Account",
  "externalId": "INV-20260830-001",
  "invoiceUrl": "https://checkout.xendit.co/..."
}
```

> **`targetType`** enum: `ORDER`, `DINE_IN`. Digunakan untuk referensikan pembayaran ke order atau sesi dining.

#### PaymentResponse
```json
{
  "id": 1,
  "targetType": "ORDER",
  "targetId": 1,
  "targetReference": "ORD-20260823-0001",
  "paymentMethodId": 1,
  "paymentMethodName": "BCA Virtual Account",
  "externalId": "INV-20260830-001",
  "invoiceUrl": "https://checkout.xendit.co/...",
  "status": "PENDING",
  "paymentChannel": "XENDIT",
  "paymentDetail": "BCA Virtual Account",
  "amount": 54000,
  "paidAt": null,
  "createdAt": "2026-08-30T10:00:00Z",
  "updatedAt": null
}
```

---

### L. Payment Methods (`/api/v1/payment-methods`)

| Method | Path | Keterangan |
|---|---|---|
| `POST /` | Create | |
| `GET /` | List | Filter: `keyword` (name/code substring) |
| `GET /{id}` | Get by ID | |
| `PUT /{id}` | Full update | |
| `PATCH /{id}` | Partial update | |
| `DELETE /{id}` | Hard delete | Response: `204 No Content` |

**PaymentMethodRequest:**
```json
{
  "code": "BCA_VA",
  "name": "BCA Virtual Account",
  "isActive": true
}
```

**PaymentMethodResponse:**
```json
{
  "id": 1,
  "code": "BCA_VA",
  "name": "BCA Virtual Account",
  "isActive": true,
  "createdAt": "2026-08-30T10:00:00Z",
  "updatedAt": null
}
```

---

### M. Dining Sessions (`/api/v1/dinings`)

| Method | Path | Keterangan |
|---|---|---|
| `POST /` | Buka sesi dining baru | |
| `GET /` | List sesi | Pagination |
| `GET /{id}` | Detail sesi | |
| `POST /{id}/orders` | Tambah order ke sesi | |
| `POST /{id}/close` | Tutup sesi | |

> **Catatan:** Tidak ada delete endpoint untuk dining.

**Open Dining Request:**
```json
{ "tableId": 1 }
```

**Add Order to Dining Request:**
```json
{
  "customerId": 1,
  "customerName": "Budi",
  "notes": "Extra es",
  "items": [
    {
      "menuId": 1,
      "quantity": 2,
      "modifiers": [
        { "modifierOptionId": 1 }
      ]
    }
  ]
}
```

**DiningResponse:**
```json
{
  "id": 1,
  "tableId": 1,
  "tableNumber": "1",
  "status": "OPEN",
  "totalPrice": 54000,
  "orders": [
    {
      "id": 1,
      "orderNumber": "ORD-20260823-0001",
      "status": "CREATED",
      "totalPrice": 54000,
      "createdAt": "2026-08-23T14:30:00Z"
    }
  ],
  "createdAt": "2026-08-23T14:30:00Z",
  "updatedAt": null,
  "closedAt": null
}
```

**Enum `DiningStatus`:** `OPEN`, `CLOSED`

---

### N. Tables (`/api/v1/tables`)

| Method | Path | Keterangan |
|---|---|---|
| `POST /` | Create table | |
| `GET /` | List tables | Filter: `keyword` (tableNumber substring) |
| `GET /{id}` | Get by ID | |
| `PUT /{id}` | Full update | |
| `PATCH /{id}` | Partial update | |
| `DELETE /{id}` | Hard delete | Response: `204 No Content` |

**DiningTableRequest:**
```json
{ "tableNumber": "1" }
```

**DiningTableResponse:**
```json
{
  "id": 1,
  "tableNumber": "1",
  "status": "AVAILABLE",
  "createdAt": "2026-08-23T00:00:00Z",
  "updatedAt": null
}
```

**Enum `TableStatus`:** `AVAILABLE`, `OCCUPIED`

---

## 7. Error Codes Reference

| ErrorCode | HTTP Status | Keterangan |
|---|---|---|
| `BAD_REQUEST` | 400 | Input tidak valid / request malformed |
| `INVALID_ARGUMENT` | 400 | Argumen tidak valid (IllegalArgumentException) |
| `MALFORMED_JSON` | 400 | JSON body tidak bisa di-parse |
| `MISSING_PARAMETER` | 400 | Parameter wajib tidak dikirim |
| `UNAUTHORIZED` | 401 | Token tidak valid / expired / tidak ada |
| `INVALID_REFRESH_TOKEN` | 401 | Refresh token tidak valid atau hilang |
| `FORBIDDEN` | 403 | Tidak punya hak akses (Spring Security) |
| `NOT_FOUND` | 404 | Data tidak ditemukan |
| `METHOD_NOT_ALLOWED` | 405 | HTTP method tidak didukung |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | Content-Type tidak didukung |
| `CONFLICT` | 409 | Duplikasi data / konflik |
| `DUPLICATE_ENTRY` | 409 | Data duplikat di database |
| `INTERNAL_SERVER_ERROR` | 500 | Error tak terduga di server |

---

## 8. Kredensial Seeder (Pengujian)

Jalankan dengan `--seed dev` atau profile `dev-seed`:

| Email | Password | Role | Keterangan |
|---|---|---|---|
| `admin@rascal.id` | `admin123` | `ADMIN` | Full akses (`*`) |
| `kasir@rascal.id` | `kasir123` | `CASHIER` | Order, kasir, baca menu |
| `waiter@rascal.id` | `waiter123` | `WAITER` | Buat order, meja, baca menu |
| `kitchen@rascal.id` | `kitchen123` | `KITCHEN` | Status order, baca menu |

---

## 9. Daftar Endpoint Lengkap (Quick Reference)

```
AUTH
POST   /api/v1/auths/login              (public)
POST   /api/v1/auths/refresh            (public, cookie only)
POST   /api/v1/auths/logout             (cookie)
POST   /api/v1/auths/logout-all         (bearer)

USERS
POST   /api/v1/auths/users
GET    /api/v1/auths/users?page=&size=&email=
GET    /api/v1/auths/users/{id}
PUT    /api/v1/auths/users/{id}
PATCH  /api/v1/auths/users/{id}
DELETE /api/v1/auths/users/{id}

ROLES
POST   /api/v1/auths/roles
GET    /api/v1/auths/roles?page=&size=&name=
GET    /api/v1/auths/roles/{id}
PUT    /api/v1/auths/roles/{id}
PATCH  /api/v1/auths/roles/{id}
DELETE /api/v1/auths/roles/{id}

AUTHORITIES
GET    /api/v1/auths/authorities?page=&size=&name=
GET    /api/v1/auths/authorities/{id}
DELETE /api/v1/auths/authorities/{id}

MENUS (V1)
POST   /api/v1/menus
GET    /api/v1/menus?page=&size=&name=&categoryId=
GET    /api/v1/menus/{id}
PUT    /api/v1/menus/{id}
PATCH  /api/v1/menus/{id}/restore
DELETE /api/v1/menus/{id}

MENUS (V2 — cached)
POST   /api/v2/menus
GET    /api/v2/menus?page=&size=&name=&categoryId=
GET    /api/v2/menus/{id}
PUT    /api/v2/menus/{id}
PATCH  /api/v2/menus/{id}/restore
DELETE /api/v2/menus/{id}

MENU CATEGORIES
POST   /api/v1/menus/categories
GET    /api/v1/menus/categories?page=&size=&name=
GET    /api/v1/menus/categories/{id}
PUT    /api/v1/menus/categories/{id}
PATCH  /api/v1/menus/categories/{id}/restore
DELETE /api/v1/menus/categories/{id}

MODIFIERS
POST   /api/v1/menus/modifiers
GET    /api/v1/menus/modifiers?page=&size=&name=
GET    /api/v1/menus/modifiers/{id}
PUT    /api/v1/menus/modifiers/{id}
DELETE /api/v1/menus/modifiers/{id}

IMAGE UPLOAD
GET    /api/v1/images/auth

ORDERS
POST   /api/v1/orders
GET    /api/v1/orders?page=&size=&keyword=&status=&paidStatus=
GET    /api/v1/orders/{id}
PUT    /api/v1/orders/{id}
PATCH  /api/v1/orders/{id}
DELETE /api/v1/orders/{id}
POST   /api/v1/orders/{id}/confirm
POST   /api/v1/orders/{id}/prepare
POST   /api/v1/orders/{id}/ready
POST   /api/v1/orders/{id}/complete
POST   /api/v1/orders/{id}/cancel

PAYMENTS
POST   /api/v1/payments
GET    /api/v1/payments?page=&size=&keyword=&targetType=&targetId=&status=&paymentMethodId=
GET    /api/v1/payments/{id}
PUT    /api/v1/payments/{id}
PATCH  /api/v1/payments/{id}
DELETE /api/v1/payments/{id}
POST   /api/v1/payments/{id}/pay
POST   /api/v1/payments/{id}/expire
POST   /api/v1/payments/{id}/fail
POST   /api/v1/payments/{id}/refund

PAYMENT METHODS
POST   /api/v1/payment-methods
GET    /api/v1/payment-methods?page=&size=&keyword=
GET    /api/v1/payment-methods/{id}
PUT    /api/v1/payment-methods/{id}
PATCH  /api/v1/payment-methods/{id}
DELETE /api/v1/payment-methods/{id}

DINING
POST   /api/v1/dinings
GET    /api/v1/dinings?page=&size=
GET    /api/v1/dinings/{id}
POST   /api/v1/dinings/{id}/orders
POST   /api/v1/dinings/{id}/close

TABLES
POST   /api/v1/tables
GET    /api/v1/tables?page=&size=&keyword=
GET    /api/v1/tables/{id}
PUT    /api/v1/tables/{id}
PATCH  /api/v1/tables/{id}
DELETE /api/v1/tables/{id}
```
