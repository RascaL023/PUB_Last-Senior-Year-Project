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
│                               global exception handler, seed framework,
│                               image service abstraction, utilities
│
├── auth-api/
│   └── jar                  ← Auth contract (AuthApi interface + DTO records)
│                               Boleh diakses oleh module eksternal
│
├── auth-core/
│   └── jar                  ← Implementasi domain auth (controllers,
│                               services, repositories, entities)
│
├── menu-api/
│   └── jar                  ← Menu contract (MenuApi interface + snapshot DTOs)
│                               Snapshot berisi modifierTypes untuk validasi modifier
│
├── menu-core/
│   └── jar                  ← Implementasi domain menu (CRUD, modifier, image upload)
│
├── order-api/
│   └── jar                  ← Order contract (OrderApi interface + DTO records)
│                               payment-core depend ke modul ini
│
├── order-core/
│   └── jar                  ← Implementasi domain order (CRUD, reconcile, status flow)
│
├── payment-core/
│   └── jar                  ← Implementasi domain payment (Payment + PaymentMethod,
│                               status flow policy, target-based referencing)
│
├── dining-api/
│   └── jar                  ← Dining contract (DiningApi interface + DTO records)
│
├── dining-core/
│   └── jar                  ← Implementasi domain dining (session, table, order item)
│
└── core-app/
    └── jar                  ← Entry point aplikasi (@SpringBootApplication)
                                Hanya bootstrapping + konfigurasi global
```

---

## Dependency Graph

```
                    ┌──────────┐
                    │  common  │ ← spring-webmvc, jackson-annotations,
                    └────┬─────┘   seed framework, image service, utilities
                         │
       ┌────────┬────────┼────────┬────────┐
       │        │        │        │        │
  ┌────▼───┐ ┌──▼──┐ ┌──▼───┐ ┌─▼──┐ ┌───▼────┐
  │auth-api│ │menu │ │order │ │dining│ │(modul  │
  └────────┘ │-api │ │-api  │ │-api │ │ lain)  │
             └──┬──┘ └──┬───┘ └──┬──┘ └────────┘
                │       │        │
  ┌─────────────┤   ┌────┘   ┌───┘
  │             │   │        │
┌─▼──────┐ ┌───▼───▼─┐  ┌──▼────────┐
│auth-core│ │menu-core│  │order-core │ ← common, menu-api, order-api
└───┬─────┘ └─────────┘  └───┬───────┘
    │                        │
    │     ┌──────────────────┤
    │     │                  │
┌───▼─────▼──────┐    ┌─────▼────────┐
│    core-app    │    │ payment-core  │ ← common, order-api
└────────────────┘    └──────────────┘
                          │
                    ┌─────▼────────┐
                    │ dining-core  │ ← common, menu-api, dining-api, order-api
                    └──────────────┘
```

### Aturan Dependensi

| Prinsip | Keterangan |
|---|---|
| `common` → tidak boleh depend ke module internal lain | Foundation layer — boleh depend ke library eksternal saja |
| `*-api` → tidak boleh depend ke module internal lain | Contract layer — hanya berisi interface + DTO records |
| `*-core` → boleh depend ke `common` dan `*-api` | Implementasi — depend ke contract layer, bukan langsung ke core lain |
| `core-app` → boleh depend ke module manapun, tapi seminimal mungkin | Bootstrap layer — hanya registrasi module |

### Contoh Arah Dependensi yang Benar

- `order-core` → `menu-api` (untuk snapshot menu) ✅
- `payment-core` → `order-api` (untuk akses order) ✅
- `payment-core` → `order-core` ❌ (tidak boleh langsung ke core lain)
- `dining-core` → `menu-api` + `order-api` ✅
- `menu-core` → `auth-api` ❌ (tidak perlu, menu tidak terkait auth)

---

## Module Contract Pattern

Setiap domain memiliki **api** dan **core** modul yang terpisah:

### Contract (`*-api`)

Berisi interface + DTO records tanpa implementasi. Modul lain depend ke sini
untuk mengakses data tanpa coupling ke JPA/repository.

```java
// Contoh: menu-api/MenuApi.java
public interface MenuApi {
    List<MenuApiResponse> getMenuSnapshots(Collection<Long> menuIds);
    List<ModifierOptionApiResponse> getModifierOptionSnapshots(Collection<Long> optionIds);
}
```

### Implementation (`*-core`)

Mengimplementasikan contract dari `*-api`. Backend **harus** mengimplementasikan
interface contract di dalam modul core-nya.

```java
// Contoh: menu-core/MenuApiImpl.java
@Component
public class MenuApiImpl implements MenuApi {
    // implementasi pakai repository, service, dsb.
}
```

### Kenapa Dipisah?

1. **Arah dependensi jelas** — `payment-core` → `order-api`, bukan `order-core`
2. **Siap extract microservice** — tinggal publish `order-api` sebagai library
3. **Tidak ada circular dependency** — modul hanya kenal contract, bukan implementasi

---

## Common Module — Fitur Shared

### Response Pattern

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
  "message": "Data on page retrieved successfully",
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
            HttpStatus.OK,
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
throw new UnauthorizedException("Authentication required");
```

---

## Exception Handling

- **Custom exceptions** (`NotFoundException`, `BadRequestException`, `ConflictException`, `UnauthorizedException`)
  ada di `common` → semua module bisa melempar tanpa import module lain
- **GlobalExceptionHandler** ada di `common` → otomatis aktif di aplikasi
  Spring Boot manapun yang menggunakan module `common`
- Handler mencakup: custom exceptions, validation errors, Spring built-in
  exceptions (bad JSON, method not allowed, missing params, data integrity violation, dll)

### Saat Extract ke Microservice

Module baru tinggal depend ke `common` → exception handler langsung berfungsi.
Tidak perlu copy/paste handler.

---

## Seed Framework

`common` menyediakan framework seeding yang bisa digunakan oleh semua module:

### Komponen

| Komponen | Lokasi | Fungsi |
|---|---|---|
| `Seeder` | `common/.../seed/Seeder.java` | Interface — setiap seeder implement `seedType()` + `seed()` |
| `SeedType` | `common/.../seed/SeedType.java` | Enum: `DEV`, `FORMAL` |
| `DatabaseSeeder` | `common/.../seed/DatabaseSeeder.java` | CommandLineRunner — resolve seed type dari arg/propfile, run ordered seeders |
| `ChunkedSeederSupport` | `common/.../seed/ChunkedSeederSupport.java` | Helper idempotent — chunked insert, skip existing |

### Cara Pakai

```java
@Component
@Order(10)
public class DevMenuSeeder implements Seeder {

    private final MenuRepository menuRepository;
    private final ChunkedSeederSupport chunkedSeeder;

    @Override
    public SeedType seedType() {
        return SeedType.DEV;
    }

    @Override
    public void seed() {
        // Seed dev data langsung atau pakai chunkedSeeder untuk bulk insert
    }
}
```

### Menjalankan Seed

```bash
# Via program argument (recommended)
java -jar app.jar --seed dev
java -jar app.jar --seed formal

# Via legacy Spring profiles
java -jar app.jar --spring.profiles.active=dev-seed
java -jar app.jar --spring.profiles.active=formal-seed
```

---

## Image Service

`common` menyediakan abstraksi untuk upload/gambar via ImageKit:

| Komponen | Fungsi |
|---|---|
| `ImageService` | Interface: `buildPath()`, `resolveUrl()`, `deleteByPath()`, `generateUploadAuth()` |
| `ImageKitImageService` | Implementasi ImageKit — resolve relative path ke URL, delete via API, signed upload auth |
| `ImageUploadAuth` | Record: publicKey, token, expire, signature |

DB hanya menyimpan **relative path** (mis. `/assets/images/menus/<code>/nama_file`).
Resolve ke URL dilakukan saat response dibangun, bukan saat penyimpanan.

### Env Vars

```
imagekit.url-endpoint=https://ik.imagekit.io/xxxx
imagekit.public-key=public_xxxx
imagekit.private-key=private_xxxx
```

---

## Utilities — StringUtil

`common/.../util/StringUtil.java` menyediakan helper string:

| Method | Fungsi |
|---|---|
| `safeIsBlank(String)` | Null-safe blank check |
| `normalizeSearch(String)` | Trim whitespace |
| `normalizeAndCapitalizeFirst(String)` | Trim + capitalize first, lowercase rest |
| `toSlug(String)` | Generate URL slug dari string |
| `normalizeSpaces(String)` | Collapse multiple spaces |
| `capitalize(String)` | Capitalize first character |
| `toUnderscoredEnum(String)` | Normalize ke UPPER_SNAKE_CASE untuk enum parsing |

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
3. Tambahkan dependency ke `common` dan module contract yang dibutuhkan
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
6. Buat `ProductApi` interface di `product-api`
7. Implement `ProductApiImpl` di `product-core`
8. Buat seeder (opsional) implement `Seeder` di `product-core`

---

## Migration Path ke Microservice

Ketika salah satu domain (misal: `order`) perlu di-extract menjadi service terpisah:

1. Buat `order-app/pom.xml` dengan parent `spring-boot-starter-parent`
2. Copy semua dependency dari `order-core/pom.xml` ke `order-app/pom.xml`
3. Tambahkan dependency `common` sebagai library (publish ke repo)
4. Publish `order-api` sebagai library untuk consumer lain (misal: `payment-core`)
5. Copy `GlobalExceptionHandler` atau tetap pakai dari `common`
6. Tambahkan `@SpringBootApplication` dan konfigurasi port

Tidak ada perubahan kode bisnis yang diperlukan.

---

## File Reference

| File | Lokasi |
|---|---|
| ApiResponse utility | `common/.../ApiResponse.java` |
| Response templates | `common/.../template/*.java` |
| Custom exceptions | `common/.../exception/*.java` (kecuali handler) |
| Exception handler | `common/.../exception/GlobalExceptionHandler.java` |
| Seed framework | `common/.../seed/*.java` |
| Image service | `common/.../image/*.java` |
| String utilities | `common/.../util/StringUtil.java` |
| Parent POM | `backend/pom.xml` |
| Entry point | `core-app/.../CoreAppApplication.java` |
