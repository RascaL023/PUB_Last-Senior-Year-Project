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

### 1. REST API `[ ]`
- [ ] Method: GET, POST, PUT, PATCH, DELETE
- [ ] Menggunakan HTTP Status Code yang sesuai

### 2. Authentication & Authorization `[ ]`
- [ ] Register
- [ ] Login
- [ ] Logout
- [ ] Refresh Token *(opsional, nilai tambah)*
- [ ] Forgot Password
- [ ] Reset Password

### 3. Role Based Access Control (RBAC) `[ ]`
- [ ] Minimal 2 role (contoh: Admin, User)
- [ ] Hak akses tiap role berbeda

### 4. CRUD Lengkap `[ ]`
- [ ] Minimal **6 entitas utama**
- [ ] Setiap entitas: Create, Read, Update, Delete
- [ ] Tidak ada CRUD yang hanya dummy

### 5. Server Side Validation `[ ]`
Semua endpoint POST & PUT wajib validasi, error dikembalikan **format JSON**:
- [ ] Required
- [ ] Email
- [ ] Unique
- [ ] Minimum
- [ ] Maximum
- [ ] Enum
- [ ] Numeric
- [ ] Date

### 6. Upload File `[ ]`
- [ ] Backend mendukung upload **Gambar** atau **PDF**

### 7. Global Error Handling `[ ]`
Response konsisten untuk:
- [ ] 400 Bad Request
- [ ] 401 Unauthorized
- [ ] 403 Forbidden
- [ ] 404 Not Found
- [ ] 422 Validation Error
- [ ] 500 Internal Server Error

### 8. Database Relationship `[ ]`
- [ ] Minimal **6 tabel utama**
- [ ] Minimal **5 relasi**
- [ ] Terdapat: One To One, One To Many, Many To One, Many To Many

### 9. Soft Delete `[ ]`
- [ ] Diterapkan minimal pada **2 tabel**
- [ ] Data yang dihapus tidak langsung hilang dari database

### 10. API Documentation `[ ]`
Salah satu berikut (dapat digunakan untuk uji seluruh endpoint):
- [ ] Swagger
- [ ] OpenAPI
- [ ] Postman Collection

### 11. Security `[ ]`
Minimal terapkan **salah satu**:
- [ ] Password Hashing
- [ ] JWT Authentication
- [ ] CORS
- [ ] Request Validation
- [ ] SQL Injection Prevention
- [ ] XSS Protection *(nilai tambah)*

### 12. Search, Filter & Pagination API `[ ]`
Endpoint list wajib mendukung:
- [ ] Search
- [ ] Filter
- [ ] Sorting
- [ ] Pagination

Contoh:
```
GET /products?page=1&limit=10
GET /products?search=laptop
GET /products?status=active
GET /products?sort=name
GET /products?category=1
```

---

## 🗄️ DATABASE

- [ ] Minimal **6 tabel utama**
- [ ] Minimal **5 relasi** antar tabel
- [ ] Memiliki **Primary Key** dan **Foreign Key**
- [ ] Normalisasi minimal hingga **3NF**
- [ ] Timestamp `created_at` & `updated_at` pada setiap tabel utama
- [ ] Minimal **2 tabel** menerapkan **soft delete**
- [ ] Data awal (seed) minimal **20 data** per tabel utama agar aplikasi dapat diuji
