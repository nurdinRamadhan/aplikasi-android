# Log Perubahan: Offline-First Cache Tagihan di Android App

## Tanggal
26 Juli 2026

## Latar Belakang Masalah
Aplikasi Android wali santri menggunakan **offline-first cache** (`OfflineFirstCacheStore`) untuk data tagihan. Cache sebelumnya **tidak memiliki TTL (Time To Live)** — data disimpan selamanya tanpa mekanisme expiry.

**Gejala:** Ketika admin generate tagihan baru (SPP & Kas Safar 1448 H jam 19:06), database sudah benar (15 records untuk Ahmad test). Tapi aplikasi Android:
1. Buka app → tampilkan **cache lama** (3 item: Tilawah, Listrik, Register)
2. Fetch server di background → 15 item (SPP & Kas baru muncul)
3. User sudah melihat data lama sebelum refresh selesai

## Root Cause
- Cache `tagihan` disimpan via `writeList()` **tanpa `expiresAt`** → never expire
- `getTagihanByNis()` emit cache dulu, lalu fetch server → race condition UI
- Tidak ada indikator apakah data **fresh** atau **stale**

## Solusi yang Diimplementasikan

### 1. Cache Model Baru: `TagihanCache` (`KeuanganModels.kt`)
```kotlin
@Serializable
data class TagihanCache(
    val items: List<TagihanWithDetail>,  // Data asli tetap utuh
    val cachedAt: Long = System.currentTimeMillis(),
    val serverSyncedAt: Long? = null,    // Kapan terakhir sync ke server
    val etag: String? = null             // Untuk conditional fetch masa depan
) {
    /** True jika data belum sync ke server > 5 menit */
    val isStale: Boolean
        get() = serverSyncedAt != null && (System.currentTimeMillis() - serverSyncedAt!!) > 5 * 60 * 1000
}
```

### 2. Cache Store: TTL Forever + Metadata (`OfflineFirstCacheStore.kt`)
```kotlin
suspend fun getTagihan(nis: String): TagihanCache? =
    readCached(key("tagihan", nis), TagihanCache.serializer())?.value

suspend fun saveTagihan(nis: String, cache: TagihanCache) {
    write(
        key = key("tagihan", nis),
        domain = "tagihan",
        serializer = TagihanCache.serializer(),
        value = cache,
        expiresAt = null  // TTL FOREVER — offline-first true
    )
}
```

### 3. Repository: Conditional Fetch + Merge (`KeuanganRepositoryImpl.kt`)
```kotlin
override fun getTagihanByNis(nis: String): Flow<TagihanCache> = flow {
    // 1. Emit cache INSTANTLY (UI tidak loading)
    val cache = cacheStore.getTagihan(nis)
    cache?.let { emit(it) }

    // 2. Cek apakah perlu fetch ulang
    val shouldFetch = cache == null || isStale(cache)  // > 5 menit
    if (!shouldFetch) return@flow  // Cache fresh → skip network

    // 3. Background fetch dengan conditional (updated_at > lastSync)
    val lastSync = cache?.serverSyncedAt
    val selectBuilder = supabaseClient.from("tagihan_santri").select(...) {
        filter { eq("santri_nis", nis) }
        if (lastSync != null && lastSync > 0) {
            gt("updated_at", lastSyncDateTime.toString())
        }
    }
    
    val fresh = selectBuilder.decodeList<TagihanWithDetail>()
    
    // 4. Merge: fresh wins, tapi keep cache yg tidak ada di fresh
    val merged = mergeWithCache(cache?.items ?: emptyList(), fresh)
    
    // 5. Save cache dengan metadata baru
    val newCache = TagihanCache(
        items = merged,
        serverSyncedAt = System.currentTimeMillis(),
        etag = generateEtag(merged)
    )
    cacheStore.saveTagihan(nis, newCache)
    emit(newCache)
}
```

### 4. ViewModel: Return `TagihanCache` (`KeuanganViewModel.kt`)
```kotlin
sealed interface TagihanUiState {
    data object Loading : TagihanUiState
    data class Success(val cache: TagihanCache) : TagihanUiState  // ← Bukan List<TagihanWithDetail>
    data class Error(val message: String) : TagihanUiState
}
```

### 5. UI: Stale Indicator Badge (`KeuanganScreen.kt`)
```kotlin
@Composable
fun StaleIndicator(cache: TagihanCache?) {
    val isStale = cache?.isStale == true
    if (!isStale) return

    val timeAgo = System.currentTimeMillis() - (cache?.serverSyncedAt ?: 0)
    val label = when {
        timeAgo < 60_000 -> "Data offline • Baru saja"
        timeAgo < 3_600_000 -> "Data offline • ${timeAgo / 60_000} menit lalu"
        timeAgo < 86_400_000 -> "Data offline • ${timeAgo / 3_600_000} jam lalu"
        else -> "Data offline • ${timeAgo / 86_400_000} hari lalu"
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(24.dp, 8.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Info, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(
            color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Medium))
    }
}
```

Digunakan di list tagihan:
```kotlin
is TagihanUiState.Success -> {
    val cache = state.cache
    if (cache.isStale) item { StaleIndicator(cache) }
    // render list...
}
```

### 6. Akses Data di UI (Tidak Breaking)
```kotlin
// Sebelum: state.tagihan
// Sekarang: state.cache.items  // ← Masih List<TagihanWithDetail> utuh

// FinancialSummaryCard
val items = tagihanState.cache.items
items.sumOf { it.nominalTagihan ?: 0L }

// TagihanCard, Detail Sheet, Filter — semuanya akses .cache.items
```

## File yang Diubah (Android)

| File | Perubahan |
|------|-----------|
| `KeuanganModels.kt` | Tambah `TagihanCache` + `isStale` |
| `OfflineFirstCacheStore.kt` | `getTagihan`/`saveTagihan` pakai `TagihanCache`, TTL forever |
| `KeuanganRepositoryImpl.kt` | Return `Flow<TagihanCache>`; instant cache → conditional fetch (`updated_at > lastSync`) → merge → emit |
| `KeuanganViewModel.kt` | `TagihanUiState.Success(cache: TagihanCache)`; `refreshData()` handle `TagihanCache` |
| `KeuanganScreen.kt` | `StaleIndicator` composable; akses `cache.items` di FinancialSummary, List, Count badge |
| `HomeViewModel.kt` | `loadLatestTagihanOrEmpty` pakai `.items` |

## File yang TIDAK Disentuh (FCM/Notification Safety)
- `NotificationRepository.kt` ❌
- `AuthRepositoryImpl.kt` (FCM token logic) ❌
- `MyFirebaseMessagingService.kt` ❌
- Semua file di `ui/notifikasi/` ❌

## Verifikasi Build
```bash
./gradlew :app:assembleDebug
# BUILD SUCCESSFUL in 2m 19s
```

## Perilaku Baru

| Skenario | Sebelum | Sekarang |
|----------|---------|----------|
| **App cold start** | Loading → fetch → tampil | **Cache instant** → background sync → update |
| **App resume (<5 menit)** | Fetch ulang | **Skip network** (cache fresh) |
| **App resume (>5 menit)** | Fetch ulang | Cache instant + **conditional fetch** (`updated_at > lastSync`) |
| **Tanpa internet** | Error / kosong | **Cache selamanya** + badge "Data offline • X menit lalu" |
| **Pull-to-refresh** | Belum ada | Repo siap (manual trigger) |

## Data Integrity
- **Tidak ada data hilang**: `TagihanCache.items` = `List<TagihanWithDetail>` utuh
- **Detail tagihan**: Masih akses `tagihan.id`, `nominalTagihan`, `sisaTagihan`, `status`, `refJenisPembayaran`, dll.
- **Merge strategy**: Fresh wins, cache preserved untuk records yg tidak ada di fresh response

## Catatan Penting
1. **TTL Forever** by design untuk offline-first true — data riwayat tagihan tidak expire
2. **Stale threshold 5 menit** — bisa diubah via `STALE_THRESHOLD_MS` di repo
3. **FCM/Notification code 100% tidak disentuh** — file terpisah, vital untuk produksi
4. **Unique constraint di DB** mencegah duplikat generate massal (sudah diterapkan)

## Next Steps (Opsional)
- [ ] Pull-to-refresh manual di UI (swipe down)
- [ ] WorkManager untuk background sync saat network reconnect
- [ ] Metrics: `cache_hit_rate`, `sync_latency`, `stale_percentage`
- [ ] FCM silent push → trigger background sync (jika diperlukan nanti)

---

## Tambahan: Panduan Implementasi Filter Bulan Hijriah (Future Feature)

Jika nanti ditambahkan **filter bulan Hijriah** di aplikasi Android (mirip admin panel), berikut detail yang harus diperhatikan:

### 1. Cache Key Harus Termasuk Filter
```kotlin
// Saat ini: key("tagihan", nis) → 1 cache per santri
// Nanti: key("tagihan", "${nis}_${hijriYear}_${hijriMonth}") → cache per santri + periode
// ATAU: simpan semua,pan SEMUA tagihan santri (unfiltered) di cache, filter dilakukan di memory

// REKOMENDASI: Simpan SEMUA (unfiltered) → filter di UI/memory
// Alasan:
// - Cache size tagihan per santri kecil (~10-20 records)
// - Filter di memory instan, tidak butuh network
// - Cache key tetap sederhana: key("tagihan", nis)
// - User bisa switch filter instan tanpa loading
```

### 2. Repository: Fetch Semua, Filter di Memory
```kotlin
// JANGAN tambah filter di query Supabase
// .filter { eq("santri_nis", nis) }
//     if (hijriMonth != null) filter { eq("hijri_month", hijriMonth) }  // ❌ JANGAN

// Tetap fetch SEMUA tagihan santri
val selectBuilder = supabaseClient.from("tagihan_santri").select(...) {
    filter { eq("santri_nis", nis) }
    order("tanggal_jatuh_tempo", Order.ASCENDING)
}

// Filter di Kotlin (instant, offline-capable)
val filtered = merged.filter { tagihan ->
    val desc = tagihan.deskripsiTagihan.lowercase()
    desc.contains("muharram") || desc.contains("safar") // parsing dari deskripsi_tagihan
    // ATAU lebih baik: simpan hijri_month/hijri_year di DB, query by column
}
```

### 3. Opsi Lebih Baik: Tambah Kolom `hijri_month` + `hijri_year` di DB
```sql
-- Migration di Supabase (admin panel side)
ALTER TABLE public.tagihan_santri 
ADD COLUMN hijri_month SMALLINT,
ADD COLUMN hijri_year INTEGER;

-- Index untuk filter cepat
CREATE INDEX idx_tagihan_hijri ON tagihan_santri(santri_nis, hijri_year, hijri_month);
```
Lalu repository bisa filter di server:
```kotlin
if (hijriYear != null && hijriMonth != null) {
    selectBuilder.filter {
        eq("hijri_year", hijriYear)
        eq("hijri_month", hijriMonth)
    }
}
```

### 4. UI: Hijri Month Picker (Consisten dengan Admin Panel)
```kotlin
// Gunakan HIJRI_MONTH_OPTIONS & HIJRI_YEAR_OPTIONS dari dateHelper (sudah ada di admin)
// Reuse logic konversi di Kotlin:
// val hijriMonths = listOf("Muharram", "Safar", "Rabi'ul Awwal", ...)
// val hijriYears = (1440..1500).toList()

// State filter
var selectedHijriYear by remember { mutableStateOf(currentHijriYear) }
var selectedHijriMonth by remember { mutableStateOf(currentHijriMonth) }

// Apply filter ke cache.items
val filteredItems = remember(cache, selectedHijriYear, selectedHijriMonth) {
    cache.items.filter { item ->
        val desc = item.deskripsiTagihan.lowercase()
        val yearMatch = desc.contains(selectedHijriYear.toString())
        val monthMatch = HIJRI_MONTH_NAMES[selectedHijriMonth].lowercase().let { desc.contains(it) }
        yearMatch && monthMatch
    }
}
```

### 5. Cache Invalidation Saat Filter Berubah
```kotlin
// Filter di memory → INSTAN, tidak perlu fetch ulang
// TAPI: Jika user pilih periode yang BELUM ada di cache (cache lama)
// → perlu fetch khusus untuk periode tsb

// Solusi: Selalu fetch SEMUA (unfiltered) di background
// Cache selalu full → filter instan di mana saja
// Jika cache kosong/expired → fetch full sekali, lalu filter bebas
```

### 6. Catatan Penting
| Aspek | Catatan |
|-------|---------|
| **Cache key** | Tetap `key("tagihan", nis)` — filter di memory |
| **Network** | Hanya 1 fetch full per santri per session |
| **Offline** | Filter tetap jalan karena data full di cache |
| **Paritas admin** | Gunakan `formatHijriPeriod(hijriYear, hijriMonth)` sama seperti admin panel |
| **Deskripsi tagihan** | Format: `"SPP Bulanan Muharram 1448 H"` → parsing reliable |
| **Jangan** | Tambah filter di query Supabase (kecuali kolom `hijri_month/year` ada di DB) |

### 7. Prioritas Implementasi
1. **Cepat**: Filter di memory dari `cache.items` (parsing `deskripsi_tagihan`)
2. **Lengkap**: Tambah kolom `hijri_month` + `hijri_year` di DB + index → filter server-side
3. **UI**: Dropdown Bulan + Tahun Hijriah (copy design admin panel)