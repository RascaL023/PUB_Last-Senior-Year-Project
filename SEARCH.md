# Search — Meilisearch Read Projection

> Cara kerja pencarian menu: Meilisearch sebagai read projection dengan
> fallback otomatis ke PostgreSQL. Detail endpoint ada di `API_CONTRACT.md`
> (bagian Menus V1 dan Admin Menus).

---

## 1. Gambaran Besar

```
Write path (source of truth)          Read path (projection)
────────────────────────────          ──────────────────────
POST/PUT/DELETE /menus                GET /menus, /admin/menus/search
        │                                       │
        ▼                                       ▼
   PostgreSQL ◄── index/sync ──► Meilisearch (`menus`)
   (menu-core)   (MenuIndexEvent)  (search-meilisearch)
```

PostgreSQL tetap source of truth untuk tulis. Setiap perubahan menu
menerbitkan `MenuIndexEvent` yang disinkronkan ke index `menus` di
Meilisearch. Operasi baca tidak query tabel menu secara langsung —
melewati `MenuSearchService` yang mencoba Meilisearch dulu.

---

## 2. Modul yang Terlibat

| Modul | Isi | Peran |
|---|---|---|
| `search-api` | `SearchApi`, `SearchApiRequest`, `SearchResponse`, `SearchHit`, `IndexSettings`, `SearchIndexInitializer`, `SearchUnavailableException` | Abstraksi — `menu-core` hanya depend ke sini, tidak ke Meilisearch langsung |
| `search-meilisearch` | `MeilisearchApiImpl`, `MeilisearchConfiguration`, `MeilisearchProperties`, `MeilisearchIndexBootstrap`, `MeilisearchJson` | Adapter — komunikasi HTTP ke server Meilisearch via `RestClient` |
| `menu-core` (service) | `MenuSearchService`, `MenuAdminApplicationService`, `MenuSearchIndexInitializer`, `MenuIndexEvent`, `MenuResponseMapper`, `model/search/*` | Konsumen — mapping request, fallback, dan sinkronisasi index |

Menambah entitas searchable baru berarti: buat `SearchIndexInitializer`
baru + dokumen search-nya, tanpa menyentuh adapter.

---

## 3. Index `menus`

Dideklarasikan di `MenuSearchIndexInitializer`:

| Setting | Nilai |
|---|---|
| Index name | `menus` |
| Searchable | `name`, `description` |
| Filterable | `categoryIds`, `basePrice`, `isAvailable`, `isDeleted` |
| Sortable | `name`, `basePrice`, `createdAt` |

Settings diterapkan otomatis ke server Meilisearch setiap aplikasi start
(`MeilisearchIndexBootstrap` pada `ApplicationReadyEvent` — ensure index,
lalu PATCH settings). Dokumen yang diindex (`MenuSearchDocument`): `id`,
`name`, `description`, `basePrice`, `isAvailable`, `isDeleted`, `deletedAt`,
`createdAt`, `updatedAt`, `imageUrls`, `categoryIds`, plus proyeksi
`categories` dan `modifierTypes`.

---

## 4. Perilaku Query

### Urutan (sorting)

- Tanpa parameter `sort`: hasil diurutkan berdasarkan **ranking relevansi
  Meilisearch** — urutannya tidak deterministik antar request.
- Dengan `sort`: hanya field sortable yang dipakai (`name`, `basePrice`,
  `createdAt`). Field lain diabaikan diam-diam oleh `buildSort`.
- Untuk tampilan yang stabil (tabel admin, daftar ber-halaman), selalu
  kirim `sort` eksplisit.

### Filter dan scope hapus

- Customer (`GET /api/v1/menus`): server selalu memaksa `isDeleted=false`.
  Menu soft-deleted tidak akan pernah muncul, tanpa perlu parameter.
- Admin (`GET /api/v1/admin/menus/search`): parameter `deleted` memakai
  `DeletedScope` — `active` (default, termasuk saat kosong), `deleted`,
  atau `all`. Nilai lain ditolak dengan `400`.

### Fallback PostgreSQL

Setiap pemanggilan search dibungkus try-catch `SearchUnavailableException`
(koneksi gagal, timeout, index hilang). Saat Meilisearch tidak tersedia,
`MenuSearchService.searchWithDatabase` menjalankan query ekuivalen ke
PostgreSQL (`MenuRepository.findSearchIdsForScope`) sehingga endpoint
tetap hidup dengan format response yang sama. Perbedaan yang mungkin
terasa: ranking relevansi digantikan urutan database.

---

## 5. Konfigurasi

| Env var | Default | Keterangan |
|---|---|---|
| `MEILISEARCH_URL` | `http://localhost:7700` | URL server Meilisearch (`http://meilisearch:7700` di docker) |
| `MEILISEARCH_API_KEY` | (kosong) | Master key — dipakai sebagai `MEILI_MASTER_KEY` container dan `api-key` aplikasi |
| `MEILI_EXTERNAL_PORT` | `7700` | Port host untuk akses dashboard/API Meilisearch dari luar docker |

Contoh nilai dev ada di `config/docker.env.example` dan
`config/native.env.example`. Properti aplikasi: `meilisearch.url`,
`meilisearch.api-key` (`core/core-app/src/main/resources/application.yml`).

---

## 6. Menambah Index Baru — Checklist

1. Buat record dokumen di `model/search/` modul domain terkait.
2. Buat class implementasi `SearchIndexInitializer` (`indexName()` +
   `indexSettings()`) — settings otomatis ter-apply saat start.
3. Terbitkan event index pada create/update/delete (contoh: `MenuIndexEvent`).
4. Tulis service pembaca dengan pola yang sama: coba `SearchApi`, tangkap
   `SearchUnavailableException`, fallback ke repository.
5. Dokumentasikan atribut searchable/filterable/sortable di sini.
