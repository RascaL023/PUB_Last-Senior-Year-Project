# KETENTUAN UMUM PEMBUATAN PROJEKAN S1

---

## 📱 FRONTEND (Client Side)

### 1. Responsive Layout `[ ]`
Website wajib responsif dan usable pada:
- [ ] **Mobile** (≤ 768px)
- [ ] **Tablet** (769px – 1024px)
- [ ] **Desktop** (> 1024px)
- [ ] Tidak ada overflow / layout rusak di setiap halaman utama

### 2. Authentication Flow `[ ]`
Alur autentikasi lengkap, minimal terdiri dari:
- [ ] Login
- [ ] Register
- [ ] Logout
- [ ] Forgot Password
- [ ] Reset Password

Ketentuan:
- [ ] Token JWT disimpan di **Local Storage** atau **Cookie**
- [ ] User belum login tidak bisa akses halaman private
- [ ] Setelah login berhasil → otomatis redirect ke **Dashboard**
- [ ] Session tetap aktif saat browser di-refresh
- [ ] Logout menghapus token & seluruh data autentikasi

### 3. Routing `[ ]`
Menggunakan **Client Side Routing**, minimal:
- [ ] Public Route
- [ ] Private Route
- [ ] Role Route (berdasarkan role)
- [ ] Redirect ketika tidak memiliki hak akses

### 4. Dashboard `[ ]`
Dashboard menampilkan data **real-time** dari backend (bukan statis), minimal berisi:
- [ ] Card Summary
- [ ] Total Data
- [ ] Statistik
- [ ] Aktivitas terbaru

### 5. CRUD Interface `[ ]`
Setiap data utama wajib memiliki halaman (terhubung ke API):
- [ ] List Data
- [ ] Detail Data
- [ ] Tambah Data
- [ ] Edit Data
- [ ] Hapus Data

### 6. Searching, Filtering & Sorting `[ ]`
- [ ] **Search** berdasarkan keyword
- [ ] **Filter**: status, kategori, tanggal
- [ ] **Sorting**: terbaru, terlama, A–Z, Z–A
- [ ] Seluruh fitur dapat digunakan secara bersamaan

### 7. Pagination `[ ]`
Data list wajib pakai pagination, minimal:
- [ ] Previous
- [ ] Next
- [ ] Nomor halaman
- [ ] Informasi jumlah data
- [ ] Pilihan jumlah data per halaman

### 8. Upload File `[ ]`
- [ ] Mendukung upload **gambar** atau **PDF**

### 9. Form Validation `[ ]`
Semua form wajib validasi, error muncul **realtime**:
- [ ] Required
- [ ] Minimum karakter
- [ ] Maximum karakter
- [ ] Format Email
- [ ] Nomor Telepon
- [ ] Password Confirmation

### 10. Notification `[ ]`
Seluruh proses CRUD wajib notifikasi via **Toast**:
- [ ] Success
- [ ] Error
- [ ] Warning
- [ ] Info

### 11. Error Handling `[ ]`
Minimal halaman:
- [ ] 401 Unauthorized
- [ ] 403 Forbidden
- [ ] 404 Not Found
- [ ] 500 Internal Server Error
- [ ] Fallback ketika API gagal

---

## 🛠️ BACKEND (Server Side)

### 1. REST API `[x]`
- [x] Method: GET, POST, PUT, PATCH, DELETE
- [x] Menggunakan HTTP Status Code yang sesuai

### 2. Authentication & Authorization `[x]`
- [x] Register (`POST /api/v1/customers/register`)
- [x] Login (`POST /api/v1/auths/login`)
- [x] Logout (`POST /api/v1/auths/logout`, `POST /api/v1/auths/logout-all`)
- [x] Refresh Token (`POST /api/v1/auths/refresh`)
- [x] Forgot Password (`POST /api/v1/auths/forgot-password`)
- [x] Reset Password (`POST /api/v1/auths/reset-password`)

### 3. Role Based Access Control (RBAC) `[x]`
- [x] Minimal 2 role (contoh: Admin, User) — ACTUAL: ADMIN, CASHIER, WAITER, KITCHEN, dll.
- [x] Hak akses tiap role berbeda — diimplementasikan via `@PreAuthorize("hasAnyAuthority(...)")`

### 4. CRUD Lengkap `[x]`
- [x] Minimal **6 entitas utama** — ACTUAL: 15+ entitas (UserAuth, Role, Authority, Employee, Customer, Menu, MenuCategory, ModifierType, Order, OrderItem, Dining, DiningTable, Invoice, InvoiceItem, Payment, dll.)
- [x] Setiap entitas: Create, Read, Update, Delete
- [x] Tidak ada CRUD yang hanya dummy

### 5. Server Side Validation `[x]`
- [x] Required (`@NotBlank`, `@NotEmpty`)
- [x] Email (`@Email`)
- [x] Unique (`@Unique` constraint di entity, `DataIntegrityViolationException` handler)
- [x] Minimum (`@Size(min=...)`, `@Min(...)`)
- [x] Maximum (`@Size(max=...)`)
- [x] Enum (`@Enumerated(EnumType.STRING)`, `fromString()` parsing)
- [x] Numeric (`@Min`, `@NotNull`)
- [x] Date (`@DateTimeFormat`)
- [x] Pattern (`@Pattern` untuk phone number)
- [x] Format JSON error dikembalikan via `GlobalExceptionHandler` + `ApiResponse`

### 6. Upload File `[x]`
- [x] Backend mendukung upload **Gambar** via ImageKit (`ImageUploadAuthController` → `GET /api/v1/images/auth` menghasilkan authentication parameter untuk upload ke ImageKit)
- Catatan: Upload tidak langsung via MultipartFile di BE, melainkan client upload langsung ke ImageKit menggunakan credential dari BE

### 7. Global Error Handling `[x]`
- [x] 400 Bad Request (`BadRequestException`, `IllegalArgumentException`, `HttpMessageNotReadableException`, `MissingServletRequestParameterException`, `InvalidDataAccessApiUsageException`)
- [x] 401 Unauthorized (`UnauthorizedException`)
- [x] 403 Forbidden (`ForbiddenException`)
- [x] 404 Not Found (`NotFoundException`, `NoResourceFoundException`)
- [x] 409 Conflict (`ConflictException`, `DataIntegrityViolationException`)
- [x] 415 Unsupported Media Type (`HttpMediaTypeNotSupportedException`)
- [x] 422 Validation Error (`MethodArgumentNotValidException` → `ApiResponse.validationError()`)
- [x] 500 Internal Server Error (`Exception` handler)
- [x] Response konsisten via `ApiResponse` (error/success/paged templates)

### 8. Database Relationship `[x]`
- [x] Minimal **6 tabel utama** — ACTUAL: 20+ tabel (auth_users, auth_roles, auth_authorities, employees, customers, menus, menu_categories, modifier_types, modifier_options, orders, order_items, order_item_modifiers, dinings, dining_tables, dining_orders, invoices, invoice_items, payments, image_metadatas, refresh_tokens, password_reset_tokens, dll.)
- [x] Minimal **5 relasi** — ACTUAL: 10+ relasi
- [x] Terdapat: One To One (Employee↔UserAuth, Customer↔UserAuth), One To Many (Order↔OrderItem, Invoice↔InvoiceItem, OrderItem↔OrderItemModifier), Many To One (OrderItem↔Order, InvoiceItem↔Invoice, Payment↔Invoice), Many To Many (UserAuth↔Role, Role↔Authority, Menu↔MenuCategory, Menu↔ModifierType)

### 9. Soft Delete `[x]`
- [x] Diterapkan minimal pada **2 tabel** — ACTUAL: 10+ tabel (auth_users, auth_roles, auth_authorities, employees, customers, menus, menu_categories, orders, dining_tables, invoices, payments)
- [x] Data yang dihapus tidak langsung hilang dari database (field `deleted_at` di set, query filter `deleted_at is null`)

### 10. API Documentation `[ ]`
- [ ] Swagger
- [ ] OpenAPI
- [ ] Postman Collection
- **Status**: BELUM TERCAPAI — Tidak ada dependency Swagger/OpenAPI di `pom.xml`, tidak ada konfigurasi, dan tidak ada Postman Collection di repo.

### 11. Security `[x]`
- [x] Password Hashing (`BCryptPasswordEncoder` dengan strength 10)
- [x] JWT Authentication (`JwtAuthFilter` di `SecurityConfig`)
- [x] CORS (ditangani oleh Vercel proxy, tidak perlu konfigurasi BE)
- [x] Request Validation (`@Valid` pada semua endpoint POST/PUT)
- [x] SQL Injection Prevention (parameterized queries via Spring Data JPA)
- [x] XSS Protection (Spring Security default protection)

### 12. Search, Filter & Pagination API `[x]`
- [x] Search — keyword parameter pada endpoint: menus, orders, customers, payments, invoices, employees, roles, authorities, menu-categories, modifiers
- [x] Filter — status, categoryId, minPrice/maxPrice, invoiceId, paymentProvider, paymentStatus, diningStatus, orderStatus, isAvailable, deletedScope
- [x] Sorting — via `Pageable` dengan `Sort.Direction`
- [x] Pagination — `@PageableDefault` pada semua endpoint list, response berisi `SuccessPagedTemplate` dengan metadata pagination

---

## 🗄️ DATABASE

- [x] Minimal **6 tabel utama** — ACTUAL: 20+ tabel
- [x] Minimal **5 relasi** antar tabel — ACTUAL: 10+ relasi
- [x] Memiliki **Primary Key** dan **Foreign Key** — semua entity memiliki `@Id @GeneratedValue`, relasi menggunakan `@JoinColumn`/`@ManyToOne`/`@OneToMany`
- [x] Normalisasi minimal hingga **3NF** — tabel terpisah untuk entitas berbeda, tidak ada data redundan
- [x] Timestamp `created_at` & `updated_at` pada setiap tabel utama
- [x] Minimal **2 tabel** menerapkan **soft delete** — ACTUAL: 10+ tabel
- [ ] Data awal (seed) minimal **20 data** per tabel utama agar aplikasi dapat diuji
  - **Status**: BELUM TERCAPAI — Seeders DEV memiliki data terbatas (4 users, 10 dining tables, beberapa menu/categories/modifiers). Formal seeders hanya membuat 1-2 entri. Perlu ditambah agar mencapai 20 data per tabel utama.
