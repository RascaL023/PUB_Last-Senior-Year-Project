# Arsitektur Modular Monolith — Backend

## Prinsip

Proyek ini adalah **modular monolith** — kode diorganisir dalam modul Maven terpisah
seperti microservice, tetapi di-deploy sebagai satu aplikasi. Setiap modul
independen dan siap di-extract menjadi microservice kapan saja.

---

## Struktur Modul

```
backend/
├── pom.xml                  ← Parent POM (extends spring-boot-starter-parent)
│                               Mengelola versi & daftar module
│
├── common/
│   └── jar                  ← Shared response format, exception classes,
│                               global exception handler
│
├── auth-api/
│   └── jar                  ← API contracts (interfaces, DTO records)
│                               Boleh diakses oleh module eksternal
│
├── auth-core/
│   └── jar                  ← Implementasi domain auth (controllers,
│                               services, repositories, entities)
│
└── core-app/
    └── jar                  ← Entry point aplikasi (@SpringBootApplication)
                                Hanya bootstrapping + konfigurasi global
```

---

## Dependency Graph

```
          ┌──────────┐
          │  common  │ ← spring-webmvc, jackson-annotations
          └────┬─────┘
               │
     ┌─────────┴──────────┐
     │                    │
┌────▼─────┐       ┌─────▼─────┐
│ auth-api │       │ auth-core │ ← common, auth-api, starter-web,
└──────────┘       └─────┬─────┘          starter-validation, starter-data-jpa
                         │
                    ┌────▼─────┐
                    │ core-app │ ← auth-core, starter-web
                    └──────────┘
```

Aturan:
- `common` → tidak boleh depend ke module internal lain
- `auth-api` → tidak boleh depend ke module internal lain
- `auth-core` → boleh depend ke `common` dan `auth-api`
- `core-app` → boleh depend ke module manapun, tapi seminimal mungkin

---

## Response Pattern

Semua response API melalui `common` module dengan format terstruktur:

### Success (single data)

```json
{
  "isSuccess": true,
  "message": "Request processed successfully",
  "data": { ... },
  "meta": {
    "timestamp": "2026-07-28T02:08:28Z"
  }
}
```

### Success (paged)

```json
{
  "isSuccess": true,
  "message": "Data retrieved successfully",
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
    "timestamp": "2026-07-28T02:08:28Z"
  }
}
```

### Error

```json
{
  "isSuccess": false,
  "message": "User not found",
  "errorCode": "NOT_FOUND",
  "meta": {
    "timestamp": "2026-07-28T02:08:28Z"
  }
}
```

### Validation Error

```json
{
  "isSuccess": false,
  "message": "Validation failed",
  "errors": [
    { "field": "email", "message": "must not be blank" }
  ],
  "meta": {
    "timestamp": "2026-07-28T02:08:28Z"
  }
}
```

### Cara Pakai di Controller

```java
@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid RoleRequest request) {
        RoleResponse data = roleService.create(request);
        return ApiResponse.success(HttpStatus.CREATED, data);
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        List<RoleResponse> data = roleService.getAll();
        return ApiResponse.success(HttpStatus.OK, data);
    }

    @GetMapping
    public ResponseEntity<?> getAllPaged(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Page<RoleResponse> pageResult = roleService.getAllPaged(page, size);
        return ApiResponse.paged(
            HttpStatus.OK, "Data retrieved",
            pageResult.getContent(),
            pageResult.getNumber() + 1,
            pageResult.getSize(),
            pageResult.getTotalElements(),
            pageResult.hasNext(),
            pageResult.hasPrevious()
        );
    }
}
```

### Cara Lempar Error

Cukup throw exception — `GlobalExceptionHandler` di `common` akan menangkap
dan mengembalikan response yang sesuai:

```java
throw new NotFoundException("User not found");
throw new BadRequestException("Invalid email");
throw new ConflictException("Role already exists");
```

---

## Exception Handling

- **Custom exceptions** (`NotFoundException`, `BadRequestException`, `ConflictException`)
  ada di `common` → semua module bisa melempar tanpa import module lain
- **GlobalExceptionHandler** ada di `common` → otomatis aktif di aplikasi
  Spring Boot manapun yang menggunakan module `common`
- Handler mencakup: custom exceptions, validation errors, Spring built-in
  exceptions (bad JSON, method not allowed, missing params, dll)

### Saat Extract ke Microservice

Module baru tinggal depend ke `common` → exception handler langsung berfungsi.
Tidak perlu copy/paste handler.

---

## Aturan Dependency Management

| Prinsip | Keterangan |
|---|---|
| Parent atur VERSI | Hanya `dependencyManagement` untuk internal module |
| Module atur DEPENDENCY | Setiap module tulis dependency yang dibutuhkan |
| Tanpa `<version>` di module | Semua versi dari spring-boot-starter-parent atau parent `dependencyManagement` |
| Tidak ada `<dependencies>` di parent | Parent hanya `dependencyManagement`, bukan dependency sebenarnya |

### Contoh: Menambah Module Baru

1. Buat direktori module (misal: `product-core/`)
2. Buat `product-core/pom.xml` dengan parent `root-backend`
3. Tambahkan dependency ke `common` dan module lain yang dibutuhkan
4. Daftarkan module di `backend/pom.xml`:
   ```xml
   <module>product-core</module>
   ```
5. Tambahkan `dependencyManagement` di parent:
   ```xml
   <dependency>
       <groupId>id.my.rascal</groupId>
       <artifactId>product-core</artifactId>
       <version>${project.version}</version>
   </dependency>
   ```

---

## Migration Path ke Microservice

Ketika `auth-core` perlu di-extract menjadi service terpisah:

1. Buat `auth-app/pom.xml` dengan parent `spring-boot-starter-parent`
2. Copy semua dependency dari `auth-core/pom.xml` ke `auth-app/pom.xml`
3. Tambahkan dependency `common` sebagai library (publish ke repo)
4. Copy `GlobalExceptionHandler` atau tetap pakai dari `common`
5. Tambahkan `@SpringBootApplication` dan konfigurasi port

Tidak ada perubahan kode bisnis yang diperlukan.

---

## File Reference

| File | Lokasi |
|---|---|
| ApiResponse utility | `common/.../ApiResponse.java` |
| Response templates | `common/.../template/*.java` |
| Custom exceptions | `common/.../exception/*.java` (kecuali handler) |
| Exception handler | `common/.../exception/GlobalExceptionHandler.java` |
| Parent POM | `backend/pom.xml` |
| Entry point | `core-app/.../CoreAppApplication.java` |
