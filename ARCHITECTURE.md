# Arsitektur Modular Monolith — Backend

## Prinsip

Proyek ini adalah **modular monolith** — kode diorganisir dalam modul Maven terpisah
seperti microservice, tetapi di-deploy sebagai satu aplikasi. Setiap modul
independen dan siap di-extract menjadi microservice kapan saja.

---

## Struktur Modul

```
backend/
├── core/pom.xml             ← Parent POM (extends spring-boot-starter-parent)
│                               Mengelola versi & daftar module
│
├── core/common/             ← Shared response format, exception classes,
│                               global exception handler, seed framework, utilities
│
├── core/auth-api/           ← Auth contract (boleh diakses modul eksternal)
├── core/auth-core/          ← Implementasi domain auth (controllers, services,
│                               repositories, entities, seeders)
│
├── core/menu-api/           ← Menu contract (MenuApi interface + snapshot DTOs)
│                               Snapshot berisi modifierTypes untuk validasi modifier
├── core/menu-core/          ← Implementasi domain menu (CRUD, modifier, category,
│                               admin search via Meilisearch read projection)
│
├── core/order-api/          ← Order contract (OrderApi interface, event + DTO records)
│                               invoice-core dan dining-core depend ke modul ini
├── core/order-core/         ← Implementasi domain order (CRUD, reconcile, status flow)
│
├── core/invoice-api/        ← Invoice contract (InvoiceApi, InvoiceReportApi, domain events)
├── core/invoice-core/       ← Implementasi billing aggregate (Invoice, InvoiceItem),
│                               listener event Order/Dining/Payment — pemilik status settlement
│
├── core/payment-api/        ← Payment contract (PaymentProcessor interface + DTOs)
├── core/payment-core/       ← Implementasi domain payment (CRUD payment, status flow,
│                               target pembayaran hanya INVOICE — settlement ke invoice)
├── core/payment-xendit/     ← Adapter Xendit (invoice + webhook, implementasi
│                               PaymentProcessor untuk provider XENDIT)
│
├── core/dining-api/         ← Dining contract (DiningApi interface + DTO records)
├── core/dining-core/        ← Implementasi domain dining (session, table, order item)
│
├── core/image-api/          ← Image contract (ImageApi, ImageRegistryApi,
│                               upload-auth & webhook payload DTOs, domain events)
├── core/image-core/         ← Registry metadata + upload-auth controller
├── core/image-imagekit/     ← Adapter ImageKit (upload auth, webhook verify & parse)
│
├── core/search-api/         ← Search abstraction (SearchApi, IndexSettings,
│                               SearchIndexInitializer, SearchUnavailableException)
├── core/search-meilisearch/ ← Adapter Meilisearch (RestClient, bootstrap settings
│                               saat ApplicationReadyEvent)
│
├── core/report-api/         ← Report contract (DashboardSummaryApiResponse untuk FE)
├── core/report-core/        ← Pembaca langsung (tanpa tabel projection/listener):
│                               agregasi lewat invoice/payment/order/dining/menu-api
│
└── core/core-app/           ← Entry point aplikasi (@SpringBootApplication)
                                Hanya bootstrapping + konfigurasi global
```

---

## Dependency Graph

```
                              ┌──────────┐
                              │  common  │ ← response format, exception handler,
                              └────┬─────┘   seed framework, utilities
                                   │
     ┌─────────┬─────────┬─────────┼──────────┬──────────┬──────────┐
     │         │         │         │          │          │          │
┌────▼───┐ ┌───▼───┐ ┌───▼───┐ ┌───▼────┐ ┌───▼────┐ ┌───▼────┐ ┌───▼──────┐
│auth-api│ │menu-  │ │order- │ │payment-│ │dining- │ │image-  │ │search-   │
│        │ │api    │ │api    │ │api     │ │api     │ │api     │ │api       │
└───┬────┘ └───┬───┘ └───┬───┘ └───┬────┘ └───┬────┘ └───┬────┘ └────┬─────┘
    │          │         │         │          │          │           │
┌───▼────┐ ┌───▼───────────┐ ┌─────▼────┐ ┌───▼────────────────┐ ┌───▼────────┐
│auth-   │ │menu-core      │ │order-core│ │payment-core        │ │search-     │
│core    │ │← common,      │ │← common, │ │← common,           │ │meilisearch │
│← common│ │  menu-api,    │ │  order-  │ │  payment-api,      │ │← search-api│
│  auth- │ │  image-api,   │ │  api,    │ │  order-api,        │ └────────────┘
│  api   │ │  search-api   │ │  menu-api│ │  dining-api        │
└───┬────┘ └───────┬───────┘ └────┬─────┘ └────────┬───────────┘
    │              │              │                │
    │              │   ┌──────────┘   ┌────────────┴───────────┐
    │              │   │              │                        │
    │              │ ┌─▼──────────┐ ┌─▼────────────┐ ┌─────────▼─────────┐
    │              │ │dining-core │ │payment-xendit│ │image-core         │
    │              │ │← common,   │ │← common,     │ │image-imagekit     │
    │              │ │  dining-   │ │  payment-api,│ │← common, image-api│
    │              │ │  api,      │ │  order-api,  │ └───────────────────┘
    │              │ │  order-api │ │  dining-api  │
    │              │ └────────────┘ └──────────────┘
    │              │
    │   ┌──────────▼───────────────────────────────────────────┐
    │   │  core-app ← auth-core, menu-core, order-core (+api), │
    │   │  payment-core (+api), payment-xendit, dining-core,   │
    │   │  image-core, image-imagekit, search-meilisearch      │
    └───┤  (hanya registrasi modul — tanpa logika bisnis)      │
        └──────────────────────────────────────────────────────┘
```

> Tidak ada modul `payment-methods` — kolom `payment_method` di entity `Payment`
> hanyalah string denormalisasi, bukan relasi. Tidak ada dependensi langsung
> antar `*-core` — selalu lewat `*-api`.
>
> Diagram di atas ilustratif; daftar dependensi yang dijaga adalah bagian
> *Contoh Arah Dependensi yang Benar* di bawah (termasuk `invoice-core` & `report-core`).

### Aturan Dependensi

| Prinsip | Keterangan |
|---|---|
| `common` → tidak boleh depend ke module internal lain | Foundation layer — boleh depend ke library eksternal saja |
| `*-api` → tidak boleh depend ke module internal lain | Contract layer — hanya berisi interface + DTO records |
| `*-core` → boleh depend ke `common` dan `*-api` | Implementasi — depend ke contract layer, bukan langsung ke core lain |
| `core-app` → boleh depend ke module manapun, tapi seminimal mungkin | Bootstrap layer — hanya registrasi module |

### Contoh Arah Dependensi yang Benar

- `order-core` → `menu-api` (untuk snapshot menu) ✅
- `payment-core` → `payment-api` + `invoice-api` (resolusi target pembayaran) ✅
- `payment-xendit` → `payment-api` ✅
- `payment-core` → `order-core` ❌ (tidak boleh langsung ke core lain)
- `invoice-core` → `invoice-api` + `order-api` + `dining-api` + `payment-api` (listener event) ✅
- `dining-core` → `dining-api` + `order-api` ✅
- `menu-core` → `menu-api` + `image-api` (resolve URL gambar) + `search-api` (read projection) ✅
- `image-core` / `image-imagekit` → `image-api` ✅
- `report-core` → `report-api` + `invoice-api` + `payment-api` + `order-api` + `dining-api` + `menu-api`
  (baca langsung lewat contract — tidak pernah akses tabel domain lain) ✅
- `search-meilisearch` → `search-api` ✅
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
    public ResponseEntity<SuccessTemplate<RoleResponse>> create(
        @Valid @RequestBody RoleRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.CREATED,
            "Role successfully created",
            roleService.create(request)
        );
    }

    @GetMapping
    public ResponseEntity<SuccessPagedTemplate<List<RoleResponse>>> getAllPaged(
        @RequestParam(required = false) String name,
        @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC)
        Pageable pageable
    ) {
        Page<RoleResponse> page = roleService.getAllPaged(name, pageable);
        return ApiResponse.paged(
            HttpStatus.OK,
            "Role successfully retrieved",
            page.getContent(),
            page.getNumber() + 1, // Pageable 0-based → response 1-based
            page.getSize(),
            page.getTotalElements(),
            page.hasNext(),
            page.hasPrevious()
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

## Image Modules

Abstraksi gambar tidak lagi di `common` — sudah diekstrak menjadi tiga modul sendiri:

| Modul | Isi | Fungsi |
|---|---|---|
| `image-api` | `ImageApi`, `ImageRegistryApi`, `ImageUploadAuthApiResponse`, `ImageWebhookApiPayload`, event `ImageCreated/Updated/DeletedEvent` | Contract — dipakai `menu-core` (resolve URL) dan consumer lain |
| `image-core` | `ImageMetadata` (entity), `ImageRegistryApiImpl`, `ImageUploadAuthController` | Registry metadata + endpoint `GET /api/v1/images/auth` |
| `image-imagekit` | `ImageKitService`, `ImageKitWebhookController`, `ImageKitProperties` | Adapter ImageKit — signed upload auth, verifikasi dan parse webhook |

DB hanya menyimpan **relative path** (mis. `/assets/images/menus/<code>/nama_file`).
Resolve ke URL dilakukan saat response dibangun, bukan saat penyimpanan.

### Env Vars

```
IMAGEKIT_BASE_URL=https://api.imagekit.io
IMAGEKIT_URL_ENDPOINT=https://ik.imagekit.io/xxxx
IMAGEKIT_PUBLIC_KEY=public_xxxx
IMAGEKIT_PRIVATE_KEY=private_xxxx
IMAGEKIT_WEBHOOKS_ENDPOINT=
IMAGEKIT_WEBHOOKS_SECRET=
```

### Cara Pakai di Service

```java
// menu-core memakai contract, bukan implementasi
private final ImageApi imageApi;

String url = imageApi.resolveUrl(relativePath);
ImageUploadAuthApiResponse auth = imageApi.getAuthenticationParameters();
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
| ApiResponse utility | `core/common/src/main/java/id/my/rascal/common/ApiResponse.java` |
| Response templates | `core/common/.../template/*.java` |
| Custom exceptions | `core/common/.../exception/*.java` (kecuali handler) |
| Exception handler | `core/common/.../exception/GlobalExceptionHandler.java` |
| Seed framework | `core/common/.../seed/*.java` |
| Image contract | `core/image-api/src/main/java/id/my/rascal/image/api/*.java` |
| Image registry + upload auth | `core/image-core/.../internal/{entity,adapter,controller}/` |
| ImageKit adapter + webhook | `core/image-imagekit/.../internal/{service,controller,component}/` |
| Search abstraction | `core/search-api/src/main/java/id/my/rascal/search/api/*.java` |
| Meilisearch adapter | `core/search-meilisearch/.../internal/{adapter,config}/` |
| Security (JWT filter lib) | Eksternal `id.rascal:filter` — bukan modul internal |
| String utilities | `core/common/.../util/StringUtil.java` |
| Parent POM | `core/pom.xml` (18 modul) |
| Entry point | `core/core-app/.../CoreAppApplication.java` |

Dokumen pendamping: `API_CONTRACT.md` (spesifikasi endpoint),
`API_CONTRACT_EN.md` (versi presisi untuk agent), `SEARCH.md` (read projection),
`AUTH.md` (token lifecycle), `DOCKER-SETUP.md` (cara menjalankan).
