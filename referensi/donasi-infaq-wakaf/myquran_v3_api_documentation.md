# Dokumentasi API MyQuran V3 — Debug Guide untuk Kotlin / Jetpack Compose

> **Tujuan dokumen ini:** Menjelaskan struktur JSON asli dari API `api.myquran.com/v3`, 
> mendiagnosis dua error yang terjadi, dan menyediakan contoh perbaikan data model Kotlin (Retrofit + Gson).

---

## 📋 Daftar Isi

1. [Base URL & Gambaran Umum](#1-base-url--gambaran-umum)
2. [Endpoint Surah](#2-endpoint-surah)
   - [2a. Error HTTP 400 — Penyebab & Fix](#2a-error-http-400--penyebab--fix)
   - [2b. GET /quran/surah/semua — Daftar Semua Surah](#2b-get-quransurahsemua--daftar-semua-surah)
   - [2c. GET /quran/surah/{nomor} — Detail Satu Surah](#2c-get-quransurahnomor--detail-satu-surah)
3. [Endpoint Juz](#3-endpoint-juz)
   - [3a. Error `expected BEGIN_OBJECT but was BEGIN_ARRAY` — Penyebab & Fix](#3a-error-expected-begin_object-but-was-begin_array--penyebab--fix)
   - [3b. GET /quran/juz/{nomor} — Detail Satu Juz](#3b-get-quranjuzjuz--detail-satu-juz)
4. [Data Class Kotlin yang Benar (Retrofit + Gson)](#4-data-class-kotlin-yang-benar-retrofit--gson)
5. [Retrofit Service Interface](#5-retrofit-service-interface)
6. [Ringkasan Semua Error & Fix](#6-ringkasan-semua-error--fix)

---

## 1. Base URL & Gambaran Umum

```
Base URL : https://api.myquran.com/v3/
```

Semua response API myquran v3 mengikuti **wrapper object** berikut:

```json
{
  "status": true,
  "request": {
    "path": "quran/surah/1"
  },
  "data": <OBJECT atau ARRAY — tergantung endpoint>
}
```

> ⚠️ **PENTING:** Nilai `data` **bisa berupa Object (`{}`) atau Array (`[]`)** tergantung endpoint-nya.  
> Inilah sumber utama error `expected BEGIN_OBJECT but was BEGIN_ARRAY`.

---

## 2. Endpoint Surah

### 2a. Error HTTP 400 — Penyebab & Fix

**❌ Endpoint yang salah (menyebabkan HTTP 400):**
```
GET https://api.myquran.com/v3/quran/surah
```
Endpoint ini **tidak valid** di v3. API tidak mengenali path tanpa parameter → server mengembalikan `400 Bad Request`.

**✅ Endpoint yang benar:**

| Kebutuhan | Endpoint yang Benar |
|---|---|
| Daftar semua surah (114) | `GET /v3/quran/surah/semua` |
| Detail 1 surah + ayat | `GET /v3/quran/surah/{nomor}` (1–114) |

---

### 2b. GET /quran/surah/semua — Daftar Semua Surah

**Request:**
```
GET https://api.myquran.com/v3/quran/surah/semua
```

**Raw JSON Response:**
```json
{
  "status": true,
  "request": {
    "path": "quran/surah/semua"
  },
  "data": [
    {
      "nomor": 1,
      "nama": "الفاتحة",
      "namaLatin": "Al-Fatihah",
      "jumlahAyat": 7,
      "tempatTurun": "Mekah",
      "arti": "Pembukaan",
      "deskripsi": "Surat Al Faatihah (Pembukaan) yang diturunkan di Mekah...",
      "audioFull": {
        "01": "https://media.qurankemenag.net/audio/Abu_Bakr_Ash-Shaatree_128kbps/001.mp3",
        "02": "https://media.qurankemenag.net/audio/AbuBakrAlShatri/001.mp3"
      }
    },
    {
      "nomor": 2,
      "nama": "البقرة",
      "namaLatin": "Al-Baqarah",
      "jumlahAyat": 286,
      "tempatTurun": "Madinah",
      "arti": "Sapi Betina",
      "deskripsi": "Surat Al Baqarah yang 286 ayat itu turun di Madinah...",
      "audioFull": {
        "01": "https://media.qurankemenag.net/audio/Abu_Bakr_Ash-Shaatree_128kbps/002.mp3",
        "02": "https://media.qurankemenag.net/audio/AbuBakrAlShatri/002.mp3"
      }
    }
    // ... total 114 item
  ]
}
```

> ✅ Perhatikan: `"data"` adalah **Array/List** `[...]`, bukan Object `{...}`

**Schema Field `data[]`:**

| Field | Tipe | Keterangan |
|---|---|---|
| `nomor` | `Int` | Nomor surah (1–114) |
| `nama` | `String` | Nama Arab |
| `namaLatin` | `String` | Transliterasi Latin |
| `jumlahAyat` | `Int` | Jumlah ayat |
| `tempatTurun` | `String` | "Mekah" atau "Madinah" |
| `arti` | `String` | Terjemahan Indonesia |
| `deskripsi` | `String` | Deskripsi surah (HTML) |
| `audioFull` | `Map<String, String>` | URL audio per qori |

---

### 2c. GET /quran/surah/{nomor} — Detail Satu Surah

**Request:**
```
GET https://api.myquran.com/v3/quran/surah/1
```

**Raw JSON Response:**
```json
{
  "status": true,
  "request": {
    "path": "quran/surah/1"
  },
  "data": {
    "nomor": 1,
    "nama": "الفاتحة",
    "namaLatin": "Al-Fatihah",
    "jumlahAyat": 7,
    "tempatTurun": "Mekah",
    "arti": "Pembukaan",
    "deskripsi": "Surat Al Faatihah (Pembukaan) yang diturunkan di Mekah...",
    "audioFull": {
      "01": "https://media.qurankemenag.net/audio/Abu_Bakr_Ash-Shaatree_128kbps/001.mp3",
      "02": "https://media.qurankemenag.net/audio/AbuBakrAlShatri/001.mp3"
    },
    "ayat": [
      {
        "nomorAyat": 1,
        "teksArab": "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        "teksLatin": "bismillāhir-raḥmānir-raḥīm(i),",
        "teksIndonesia": "Dengan nama Allah Yang Maha Pengasih, Maha Penyayang.",
        "audio": {
          "01": "https://media.qurankemenag.net/audio/Abu_Bakr_Ash-Shaatree_128kbps/001001.mp3",
          "02": "https://media.qurankemenag.net/audio/AbuBakrAlShatri/001001.mp3"
        }
      },
      {
        "nomorAyat": 2,
        "teksArab": "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
        "teksLatin": "al-ḥamdu lillāhi rabbil-'ālamīn(a),",
        "teksIndonesia": "Segala puji bagi Allah, Tuhan seluruh alam,",
        "audio": {
          "01": "https://media.qurankemenag.net/audio/Abu_Bakr_Ash-Shaatree_128kbps/001002.mp3",
          "02": "https://media.qurankemenag.net/audio/AbuBakrAlShatri/001002.mp3"
        }
      }
      // ... lanjut sampai ayat ke-7
    ],
    "suratSelanjutnya": {
      "nomor": 2,
      "nama": "البقرة",
      "namaLatin": "Al-Baqarah",
      "jumlahAyat": 286
    },
    "suratSebelumnya": false
  }
}
```

> ✅ `"data"` di sini adalah **Object** `{...}`, bukan Array.

---

## 3. Endpoint Juz

### 3a. Error `expected BEGIN_OBJECT but was BEGIN_ARRAY` — Penyebab & Fix

**Error lengkap:**
```
java.lang.IllegalStateException: expected BEGIN_OBJECT but was BEGIN_ARRAY 
at line 1 column 24 path $.data
```

**Penyebab:** Data class Kotlin mendefinisikan `data` sebagai sebuah Object, padahal di response `/v3/quran/juz/{nomor}`, field `data` berisi **Array of Object** (list ayat dari juz tersebut).

**❌ Data class yang salah:**
```kotlin
data class JuzResponse(
    val status: Boolean,
    val request: RequestInfo,
    val data: JuzData   // ← SALAH: ini mengharapkan Object {}
)
```

**✅ Data class yang benar:**
```kotlin
data class JuzResponse(
    val status: Boolean,
    val request: RequestInfo,
    val data: List<JuzAyat>   // ← BENAR: data adalah Array []
)
```

---

### 3b. GET /quran/juz/{nomor} — Detail Satu Juz

**Request:**
```
GET https://api.myquran.com/v3/quran/juz/1
```

**Raw JSON Response:**
```json
{
  "status": true,
  "request": {
    "path": "quran/juz/1"
  },
  "data": [
    {
      "nomor": 1,
      "nama": "الفاتحة",
      "namaLatin": "Al-Fatihah",
      "jumlahAyat": 7,
      "ayat": [
        {
          "nomorAyat": 1,
          "teksArab": "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
          "teksLatin": "bismillāhir-raḥmānir-raḥīm(i),",
          "teksIndonesia": "Dengan nama Allah Yang Maha Pengasih, Maha Penyayang.",
          "audio": {
            "01": "https://media.qurankemenag.net/audio/Abu_Bakr_Ash-Shaatree_128kbps/001001.mp3"
          }
        }
        // ... ayat-ayat surah Al-Fatihah
      ]
    },
    {
      "nomor": 2,
      "nama": "البقرة",
      "namaLatin": "Al-Baqarah",
      "jumlahAyat": 141,
      "ayat": [
        {
          "nomorAyat": 1,
          "teksArab": "الم",
          "teksLatin": "alif lām mīm",
          "teksIndonesia": "Alif Lam Mim.",
          "audio": {
            "01": "https://media.qurankemenag.net/audio/Abu_Bakr_Ash-Shaatree_128kbps/002001.mp3"
          }
        }
        // ... ayat-ayat dalam juz 1 bagian Al-Baqarah
      ]
    }
  ]
}
```

> ✅ `"data"` adalah **Array of Object (Surah)** yang memuat surah-surah dalam juz tersebut, bukan satu Object tunggal.

**Schema Field `data[]` (per surah dalam juz):**

| Field | Tipe | Keterangan |
|---|---|---|
| `nomor` | `Int` | Nomor surah |
| `nama` | `String` | Nama Arab |
| `namaLatin` | `String` | Transliterasi Latin |
| `jumlahAyat` | `Int` | Jumlah ayat surah dalam juz ini |
| `ayat` | `List<Ayat>` | List ayat dari surah tersebut yang masuk dalam juz |

**Schema Field `ayat[]`:**

| Field | Tipe | Keterangan |
|---|---|---|
| `nomorAyat` | `Int` | Nomor ayat |
| `teksArab` | `String` | Teks Arab |
| `teksLatin` | `String` | Transliterasi Latin |
| `teksIndonesia` | `String` | Terjemahan Indonesia |
| `audio` | `Map<String, String>` | URL audio per qori |

---

## 4. Data Class Kotlin yang Benar (Retrofit + Gson)

```kotlin
// ─────────────────────────────────────────────
// Shared / Common
// ─────────────────────────────────────────────

data class RequestInfo(
    val path: String
)

data class Ayat(
    val nomorAyat: Int,
    val teksArab: String,
    val teksLatin: String,
    val teksIndonesia: String,
    val audio: Map<String, String>
)

// ─────────────────────────────────────────────
// Endpoint: GET /quran/surah/semua
// data = List<SurahItem>  ← ARRAY
// ─────────────────────────────────────────────

data class SurahListResponse(
    val status: Boolean,
    val request: RequestInfo,
    val data: List<SurahItem>          // ← List, bukan Object tunggal
)

data class SurahItem(
    val nomor: Int,
    val nama: String,
    val namaLatin: String,
    val jumlahAyat: Int,
    val tempatTurun: String,
    val arti: String,
    val deskripsi: String,
    val audioFull: Map<String, String>
)

// ─────────────────────────────────────────────
// Endpoint: GET /quran/surah/{nomor}
// data = SurahDetail  ← OBJECT tunggal
// ─────────────────────────────────────────────

data class SurahDetailResponse(
    val status: Boolean,
    val request: RequestInfo,
    val data: SurahDetail              // ← Object tunggal
)

data class SurahDetail(
    val nomor: Int,
    val nama: String,
    val namaLatin: String,
    val jumlahAyat: Int,
    val tempatTurun: String,
    val arti: String,
    val deskripsi: String,
    val audioFull: Map<String, String>,
    val ayat: List<Ayat>,
    val suratSelanjutnya: SurahItem?,  // bisa null (false dari JSON)
    val suratSebelumnya: SurahItem?    // bisa null (false dari JSON)
)

// ─────────────────────────────────────────────
// Endpoint: GET /quran/juz/{nomor}
// data = List<JuzSurahItem>  ← ARRAY of surah
// ─────────────────────────────────────────────

data class JuzResponse(
    val status: Boolean,
    val request: RequestInfo,
    val data: List<JuzSurahItem>       // ← PERBAIKAN: List bukan Object
)

data class JuzSurahItem(
    val nomor: Int,
    val nama: String,
    val namaLatin: String,
    val jumlahAyat: Int,
    val ayat: List<Ayat>
)
```

> ⚠️ **Catatan `suratSelanjutnya` / `suratSebelumnya`:**  
> API mengembalikan `false` (Boolean) jika tidak ada surah sebelum/sesudah.  
> Gson akan gagal parse ini jika field dideklarasikan sebagai `SurahItem`.  
> Gunakan custom deserializer, atau cukup anotasikan dengan `@SerializedName` dan jadikan `Any?` lalu cast manual.

**Solusi aman untuk field `false`:**
```kotlin
// Tambahkan custom deserializer di Gson builder
val gson = GsonBuilder()
    .registerTypeAdapter(SurahItem::class.java, SurahItemDeserializer())
    .create()

class SurahItemDeserializer : JsonDeserializer<SurahItem?> {
    override fun deserialize(
        json: JsonElement, typeOfT: Type, context: JsonDeserializationContext
    ): SurahItem? {
        return if (json.isJsonObject) {
            context.deserialize(json, SurahItem::class.java)
        } else null  // handle `false` → null
    }
}
```

---

## 5. Retrofit Service Interface

```kotlin
interface MyQuranApiService {

    // ✅ Daftar semua surah — WAJIB pakai /semua
    @GET("quran/surah/semua")
    suspend fun getAllSurah(): SurahListResponse

    // ✅ Detail satu surah + ayat
    @GET("quran/surah/{nomor}")
    suspend fun getSurahDetail(
        @Path("nomor") nomor: Int
    ): SurahDetailResponse

    // ✅ Detail satu juz (data = List)
    @GET("quran/juz/{nomor}")
    suspend fun getJuz(
        @Path("nomor") nomor: Int  // 1–30
    ): JuzResponse
}
```

**Retrofit setup:**
```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl("https://api.myquran.com/v3/")
    .addConverterFactory(GsonConverterFactory.create(gson))
    .build()

val api = retrofit.create(MyQuranApiService::class.java)
```

---

## 6. Ringkasan Semua Error & Fix

| # | Error | Penyebab | Fix |
|---|---|---|---|
| 1 | `HTTP 400` di halaman Surah | Memanggil `/v3/quran/surah` tanpa path tambahan | Ganti ke `/v3/quran/surah/semua` untuk list, atau `/v3/quran/surah/{nomor}` untuk detail |
| 2 | `expected BEGIN_OBJECT but was BEGIN_ARRAY at $.data` di halaman Juz | Data class Kotlin mendeklarasikan `data` sebagai Object (`JuzData`), padahal response mengembalikan Array (`[...]`) | Ubah tipe `data` di `JuzResponse` dari `JuzData` menjadi `List<JuzSurahItem>` |

---

## 7. Tabel Perbandingan Tipe `data` per Endpoint

| Endpoint | Tipe `data` | Perlu `List<>` |
|---|---|---|
| `GET /quran/surah/semua` | **Array** `[...]` | ✅ Ya |
| `GET /quran/surah/{nomor}` | **Object** `{...}` | ❌ Tidak |
| `GET /quran/juz/{nomor}` | **Array** `[...]` (list surah dalam juz) | ✅ Ya |

---

*Dokumen ini dibuat untuk membantu Gemini CLI memperbaiki integrasi API myquran v3 pada project Kotlin Jetpack Compose.*  
*API Reference resmi: https://api.myquran.com/doc*
