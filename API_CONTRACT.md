# API Contract - Backend Modular Monolith

Dokumen ini berisi spesifikasi API, format request/response, alur autentikasi, serta konvensi yang digunakan pada sistem backend ini agar tim Frontend (FE) dapat berintegrasi dengan lancar.

---

## 1. Informasi Dasar & Konvensi

*   **Port Default:** `8081` (misal: `http://localhost:8081`)
*   **Path API:** `/api/v1` dan `/api/v2` (untuk Menu V2)
*   **CORS (Cross-Origin Resource Sharing) & Proxy:** CORS ditangani di level reverse-proxy Nginx (port `9000` di lingkungan development).
    *   Nginx mengarahkan request `/api/` ke backend port `8081` dan `/` ke frontend port `5173`.
    *   Nginx menyuntikkan header CORS secara otomatis untuk origin `http://localhost:5173` (serta langsung menjawab request preflight `OPTIONS` dengan status `204`). FE dapat menembak port proxy `9000` secara bebas dari server dev Vite.
*   **Format Data:** `application/json` untuk semua request body dan response body.

### Konvensi Query Params & Pagination
Sistem menggunakan modul Spring Data Pageable default:
*   **Request Params:**
    *   `page`: **0-based** index (Halaman pertama dimulai dari `0`).
    *   `size`: Jumlah data per halaman (default `10`).
    *   `sort`: Properti yang diurutkan beserta arahnya, format `namaKolom,arah` (contoh: `name,asc` atau `createdAt,desc`).
*   **Response Metadata:**
    *   Mengembalikan halaman dengan format **1-based** (halaman pertama tertulis `1` di field `currentPage`).

---

## 2. Format Response Standar

Semua endpoint mengembalikan struktur response seragam yang dibungkus oleh template response global.

### A. Response Sukses Biasa (SuccessTemplate)
```json
{
  "isSuccess": true,
  "message": "User created",
  "data": { ... },
  "meta": {
    "timestamp": "2026-08-23T14:30:00Z"
  }
}
```

### B. Response Sukses Paginasi (SuccessPagedTemplate)
Mengandung objek `pagination` di dalam `meta`.
```json
{
  "isSuccess": true,
  "message": "Roles retrieved",
  "data": [ ... ],
  "meta": {
    "pagination": {
      "currentPage": 1,
      "perPage": 10,
      "totalItems": 4,
      "totalPages": 1,
      "hasNextPage": false,
      "hasPrevPage": false
    },
    "timestamp": "2026-08-23T14:30:00Z"
  }
}
```

### C. Response Error Umum (ErrorTemplate)
Digunakan untuk error seperti 404 (Not Found), 400 (Bad Request), 409 (Conflict), dll.
```json
{
  "isSuccess": false,
  "message": "Username/password salah",
  "errorCode": "BAD_REQUEST",
  "errors": null,
  "meta": {
    "timestamp": "2026-08-23T14:30:00Z"
  }
}
```

### D. Response Error Validasi (Validation Error)
Dikembalikan ketika input body tidak memenuhi anotasi validasi (biasanya status `400 Bad Request`). List error ditaruh di field `errors`.
```json
{
  "isSuccess": false,
  "message": "Validation failed",
  "errorCode": null,
  "errors": [
    {
      "field": "email",
      "message": "Invalid email format"
    },
    {
      "field": "password",
      "message": "Password must be at least 8 characters"
    }
  ],
  "meta": {
    "timestamp": "2026-08-23T14:30:00Z"
  }
}
```

---

## 3. Sistem Autentikasi & Otorisasi

Sistem menggunakan autentikasi berbasis **JWT (JSON Web Token)** yang dipadukan dengan **Refresh Token** via HTTP-Only Cookie.

### Mekanisme Token
1.  **Access Token:**
    *   Tipe: JWT token berumur pendek (**15 menit**).
    *   Dikirim oleh FE via header: `Authorization: Bearer <access_token>`.
    *   Claims di dalam payload:
        *   `sub`: ID User (tipe string, misal `"1"`).
        *   `roles`: Array nama role (contoh: `["ADMIN"]`).
        *   `authorities`: Array nama permission (contoh: `["menu.create", "order.read"]`).
2.  **Refresh Token:**
    *   Tipe: Token acak berumur panjang (**20 hari**).
    *   Disimpan di cookie browser bernama `refresh_token` dengan flag `httpOnly(true)`, `sameSite("Strict")`, `path("/api/v1/auths")`.

### Penanganan Kegagalan Filter Token (JwtAuthFilter)
*   **Tanpa Header Authorization / Bukan Bearer:** Request akan melompati filter token secara pasif. Jika endpoint membutuhkan autentikasi (bukan path login), request akan dihadang oleh Spring Security dengan status `401 Unauthorized`.
*   **Token Kedaluwarsa:** Langsung mengembalikan `401 Unauthorized` dengan pesan `"Expired token"` dan `errorCode: "UNAUTHORIZED"`.
*   **Token Tidak Valid / Rusak:** Langsung mengembalikan `401 Unauthorized` dengan pesan `"Invalid token"` dan `errorCode: "UNAUTHORIZED"`.

> **Catatan Penting:**
> Endpoint `/api/v1/auths/refresh` bersifat **publik (permitAll)** dan dibebaskan dari pemeriksaan `JwtAuthFilter`. FE dapat memanggil endpoint ini hanya dengan menyertakan Cookie `refresh_token` tanpa harus mengirimkan header `Authorization` yang masih aktif (atau bahkan jika access token lama sudah kedaluwarsa).

---

## 4. Matriks Otorisasi (Authority Matrix)

Bebrapa endpoint dilindungi oleh otorisasi spesifik menggunakan anotasi `@PreAuthorize`. Berikut daftar hak akses yang dibutuhkan:

| Endpoint | Otorisasi yang Dibutuhkan | Keterangan |
| :--- | :--- | :--- |
| **Auths** (Login / Logout / Refresh) | - / `permitAll()` | Login bersifat publik, refresh & logout butuh login |
| **Users** (CRUD) | Bebas / Cukup Login | Tidak ada filter otorisasi khusus untuk aksi user |
| **Authorities (Read/Delete)** | `authority.create` atau `authority.*` (untuk GetById)<br>`authority.read` atau `authority.*` (untuk List)<br>`authority.delete` atau `authority.*` (untuk Delete) | *Quirk:* Aksi mengambil detail ID membutuhkan hak akses `create` |
| **Roles (CRUD)** | `role.create` atau `role.*` (untuk Create)<br>`role.read` or `role.*` (untuk GetById & List)<br>`role.update` or `role.*` (untuk Update/Patch)<br>`role.delete` or `role.*` (untuk Delete) | Pengaturan role terproteksi penuh |
| **Menus, Categories, Modifiers** | Bebas / Cukup Login | Pengelolaan menu tidak dikunci per-authority |
| **Orders** | Bebas / Cukup Login | Aksi order/transisi status tidak dikunci per-authority |

---

## 5. Referensi Endpoint API

### A. Modul Autentikasi (`/api/v1/auths`)

#### 1. Login
*   **Method / Path:** `POST /api/v1/auths/login`
*   **Auth Required:** No (Public)
*   **Request Body (LoginRequest):**
    ```json
    {
      "email": "admin@rascal.id",
      "password": "admin123"
    }
    ```
*   **Response (200 OK):**
    Mengembalikan data user dan token, serta menyetel cookie `refresh_token`.
    ```json
    {
      "isSuccess": true,
      "message": "Login success, welcome back!",
      "data": {
        "id": 1,
        "email": "admin@rascal.id",
        "accessToken": "eyJraWQiOiJhdGxhbnRh..."
      },
      "meta": {
        "timestamp": "2026-08-23T14:30:00Z"
      }
    }
    ```

#### 2. Refresh Token
*   **Method / Path:** `POST /api/v1/auths/refresh`
*   **Auth Required:** Cookie-based (Hanya butuh Cookie `refresh_token`, header Authorization bebas dikirim/diabaikan)
*   **Response (200 OK):**
    ```json
    {
      "isSuccess": true,
      "message": "Request processed successfully",
      "data": {
        "accessToken": "new_access_token_here"
      },
      "meta": {
        "timestamp": "2026-08-23T14:30:00Z"
      }
    }
    ```

#### 3. Logout
*   **Method / Path:** `POST /api/v1/auths/logout`
*   **Auth Required:** Yes (Membaca Cookie `refresh_token`)
*   **Response (200 OK):**
    Akan menghapus cookie `refresh_token` dari browser.
    ```json
    {
      "isSuccess": true,
      "message": "Logout success",
      "data": null,
      "meta": { "timestamp": "2026-08-23T14:30:00Z" }
    }
    ```

#### 4. Logout dari Semua Device
*   **Method / Path:** `POST /api/v1/auths/logout-all`
*   **Auth Required:** Yes (Bearer Token)
*   **Response (200 OK):**
    Menghapus semua refresh token milik user di database dan menghapus cookie lokal.
    ```json
    {
      "isSuccess": true,
      "message": "Logout all success",
      "data": null,
      "meta": { "timestamp": "2026-08-23T14:30:00Z" }
    }
    ```

---

### B. Modul User & Role Management

#### 1. Users (`/api/v1/auths/users`)
*   **Create User (`POST /`):**
    *   Request:
        ```json
        {
          "email": "user@rascal.id",
          "password": "securepassword123",
          "roleIds": [1]
        }
        ```
    *   Response (201 Created): `UserAuthResponse`
*   **Get User By ID (`GET /{id}`):**
    *   Response: `UserAuthResponse`
*   **List Users (`GET /?email=&page=&size=&sort=`):**
    *   Filter pencarian opsional berdasarkan substring `email`.
    *   Response: Paged list of `UserAuthResponse`.
*   **Update User Full (`PUT /{id}`):**
    *   Mengganti seluruh resource user. Body request sama dengan Create User.
*   **Update User Partial (`PATCH /{id}`):**
    *   Request:
        ```json
        {
          "email": "new-email@rascal.id"
        }
        ```
        *(Dapat mengirim email saja, password saja, atau roleIds saja. Kirim kosong akan memicu 400 Bad Request).*
*   **Delete User Soft (`DELETE /{id}`):**
    *   Menghapus secara logika (soft-delete).
    *   Response (200 OK): `{"isSuccess": true, "message": "User deleted", "data": null, ...}`

#### Schema: `UserAuthResponse`
```json
{
  "id": 1,
  "email": "user@rascal.id",
  "roles": [
    {
      "id": 1,
      "name": "ADMIN",
      "authorities": [
        {
          "id": 1,
          "name": "menu.create",
          "createdAt": "2026-08-23T00:00:00Z",
          "updatedAt": null
        }
      ],
      "createdAt": "2026-08-23T00:00:00Z",
      "updatedAt": null
    }
  ],
  "createdAt": "2026-08-23T00:00:00Z",
  "updatedAt": null
}
```

#### 2. Roles (`/api/v1/auths/roles`)
*   **Create Role (`POST /`):**
    *   Request:
        ```json
        {
          "name": "CASHIER",
          "authorityIds": [1, 2, 3]
        }
        ```
    *   Response (201 Created): `RoleResponse`
*   **Get Role By ID (`GET /{id}`):**
    *   Response: `RoleResponse`
*   **List Roles (`GET /?name=&page=&size=&sort=`):**
    *   Response: Paged list of `RoleResponse`.
*   **Update Role Full (`PUT /{id}`):**
    *   Body request sama seperti Create.
*   **Update Role Partial (`PATCH /{id}`):**
    *   Partial update `name` dan/atau `authorityIds`.
*   **Delete Role (`DELETE /{id}`):**
    *   Soft-delete role.
    *   Response (200 OK) data null.

#### 3. Authorities (`/api/v1/auths/authorities`)
*   **Get Authority By ID (`GET /{id}`):**
    *   Response: `AuthorityResponse`
*   **List Authorities (`GET /?name=&page=&size=&sort=`):**
    *   Response: Paged list of `AuthorityResponse`.
*   **Delete Authority (`DELETE /{id}`):**
    *   Response (200 OK).

---

### C. Modul Menu (`/api/v1/menus` & `/api/v2/menus`)

Sistem menyediakan dua versi endpoint untuk Menu:
*   **V1:** Mengembalikan data detail lengkap menu beserta kategori, gambar, dan pilihan modifier-nya. Cocok untuk operasi CRUD admin atau detail view.
*   **V2:** Mengembalikan skema minimalis (hanya ID relasi) untuk efisiensi caching di sisi frontend.

#### 1. Menus V1 (`/api/v1/menus`)
*   **Create Menu (`POST /`):**
    *   Request (Perhatikan huruf besar pada properti `ModifierTypeIds`):
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
    *   Response (201 Created): `MenuResponse`
*   **List Menus (`GET /?name=&categoryId=&page=&size=&sort=`):**
    *   Filter optional berdasarkan `name` (substring) dan `categoryId`.
    *   Response: Paged list of `MenuResponse`
*   **Get Menu By ID (`GET /{id}`):**
    *   Response: `MenuResponse`
*   **Update Menu (`PUT /{id}`):**
    *   Body sama dengan Create Menu.
*   **Restore Menu (`PATCH /{id}/restore`):**
    *   Mengaktifkan kembali menu yang di-soft-delete.
*   **Delete Menu (`DELETE /{id}`):**
    *   Soft-delete.
    *   Response (204 No Content) tanpa body.

##### Schema: `MenuResponse` (V1)
```json
{
  "id": 1,
  "name": "Kopi Latte",
  "description": "Kopi dengan susu segar",
  "categories": [
    {
      "id": 1,
      "displayName": "Hot Drinks",
      "categoryCode": "hot-drinks",
      "displayOrder": 1
    }
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
        {
          "id": 1,
          "name": "Normal",
          "additionalPrice": 0
        },
        {
          "id": 2,
          "name": "Less Sugar",
          "additionalPrice": 0
        }
      ]
    }
  ]
}
```

#### 2. Menus V2 (`/api/v2/menus`)
Endpoint V2 memilki path mapping, request, method, dan logic CRUD yang **identik** dengan V1. Namun, format response diperkecil hanya mengembalikan ID relasi saja (untuk di-mapping ke cache lokal milik FE).

##### Schema: `MenuResponseCached` (V2)
Response dari semua method GET/POST/PUT di V2 berbentuk seperti ini:
```json
{
  "id": 1,
  "categoryIds": [1],
  "modifierTypesIds": [1]
}
```

#### 3. Menu Categories (`/api/v1/menus/categories`)
*   **Create (`POST /`):**
    *   Request:
        ```json
        {
          "displayName": "Hot Drinks",
          "categoryCode": "hot-drinks",
          "displayOrder": 1
        }
        ```
        *(Kategori code harus berformat slug huruf kecil, contoh: `hot-drinks`)*
*   **List / GetById / Update / Delete / Restore:**
    *   Delete mengembalikan `204 No Content`. Restore mengaktifkan kembali kategori yang dihapus.

#### 4. Modifiers (`/api/v1/menus/modifiers`)
Mengelola modifier yang bisa ditempelkan ke menu (contoh ukuran cup, rasa tambahan, dll).
*   **Create (`POST /`):**
    *   Request:
        ```json
        {
          "name": "Sugar Level",
          "minSelection": 1,
          "maxSelection": 1,
          "options": [
            {
              "name": "Normal",
              "additionalPrice": 0
            },
            {
              "name": "Extra",
              "additionalPrice": 2000
            }
          ]
        }
        ```
*   **List / GetById / Update / Delete:**
    *   Delete mengembalikan `204 No Content`.

---

### D. Modul Image Upload Auth (`/api/v1/images/auth`)

Sistem mengizinkan frontend untuk melakukan upload file gambar secara langsung (*client-side upload*) ke layanan penyimpanan eksternal. Backend menyediakan kredensial bertanda-tangan (signature) agar frontend tidak perlu menyimpan private key.

*   **Get Upload Credentials (`GET /`):**
    *   **Response (200 OK):**
        ```json
        {
          "isSuccess": true,
          "message": "Upload credentials successfully generated",
          "data": {
            "publicKey": "your_image_kit_public_key",
            "token": "generated_uuid_token",
            "expire": 1787163445,
            "signature": "hmac_signature_here"
          },
          "meta": { "timestamp": "2026-08-23T14:30:00Z" }
        }
        ```

---

### E. Modul Order / Pemesanan (`/api/v1/orders`)

Modul ini mengurusi pembuatan pesanan dan alur status pengerjaan pesanan.

#### Alur Transisi Status Order:
```
  [CREATED] ──(prepare)──> [PREPARING] ──(ready)──> [READY] ──(complete)──> [COMPLETED]
      │
   (cancel)
      │
      v
  [CANCELLED]
```

#### 1. Create Order
*   **Method / Path:** `POST /api/v1/orders`
*   **Request Body (OrderRequest):**
    ```json
    {
      "customerId": 1,
      "customerName": "John Doe",
      "notes": "Pedas sedang, minta sendok",
      "items": [
        {
          "menuId": 1,
          "quantity": 2,
          "modifiers": [
            {
              "modifierOptionId": 1
            }
          ]
        }
      ]
    }
    ```
*   **Response (201 Created):** `OrderResponse`

#### 2. Transisi Status Order
Setiap aksi akan mengubah `status` pemesanan dan mengembalikan objek `OrderResponse` terbaru:
*   **Mark to Preparing:** `POST /api/v1/orders/{id}/prepare`
*   **Mark to Ready (Siap Disajikan):** `POST /api/v1/orders/{id}/ready`
*   **Mark to Complete (Selesai):** `POST /api/v1/orders/{id}/complete`
*   **Cancel Order:** `POST /api/v1/orders/{id}/cancel`

#### 3. Operasi CRUD Lain
*   **Get Order By ID (`GET /{id}`):**
    *   Response: `OrderResponse`
*   **List Orders (`GET /?keyword=&status=&page=&size=&sort=`):**
    *   Filter opsional pencarian berdasarkan substring `keyword` (mencocokkan nomor order/nama pelanggan) dan enum `status`.
    *   Response: Paged list of `OrderResponse` (default sort: `createdAt,desc`).
*   **Update Order (`PUT /{id}`):**
    *   Body sama dengan Create.
*   **Patch Order (`PATCH /{id}`):**
    *   Update parsial field `status`, `customerName`, `notes`, atau list `items`.
*   **Delete Order (`DELETE /{id}`):**
    *   Hard-delete order dari database.
    *   Response: `204 No Content`.

##### Schema: `OrderResponse`
```json
{
  "id": 1,
  "orderNumber": "ORD-20260823-0001",
  "status": "CREATED",
  "customerId": 1,
  "customerName": "John Doe",
  "notes": "Pedas sedang, minta sendok",
  "totalPrice": 50000,
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
          "name": "Normal Sugar",
          "additionalPrice": 0
        }
      ]
    }
  ]
}
```

---

## 6. Kredensial Akun Seeder (Untuk Pengujian)

Jika database di-seeding menggunakan profil `dev-seed` atau argumen `--seed dev`, akun-akun berikut dapat langsung dipakai untuk login:

| Email | Password | Role Utama | Hak Akses Utama |
| :--- | :--- | :--- | :--- |
| `admin@rascal.id` | `admin123` | `ADMIN` | Memiliki semua hak akses (`*`) |
| `kasir@rascal.id` | `kasir123` | `CASHIER` | Kelola order, kasir, baca menu |
| `waiter@rascal.id` | `waiter123` | `WAITER` | Buat order, manajemen meja, baca menu |
| `kitchen@rascal.id` | `kitchen123` | `KITCHEN` | Kelola status antrean dapur, baca menu |
