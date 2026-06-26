package com.alhasanah.alhasanahmedia.data.model.quran

import com.google.gson.annotations.SerializedName

// ─────────────────────────────────────────────
// Shared / Common
// ─────────────────────────────────────────────

data class Ayah(
    @SerializedName("id")
    val id: Int,
    @SerializedName("surah_number")
    val surahNumber: Int,
    @SerializedName("ayah_number")
    val ayahNumber: Int,
    @SerializedName("arab")
    val arab: String,
    @SerializedName("translation")
    val translation: String,
    @SerializedName("audio_url")
    val audioUrl: String? = null, // fallback
    @SerializedName("audio")
    val audio: Map<String, String>? = null, // for multi-qori
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("tafsir")
    val tafsir: Tafsir? = null,
    @SerializedName("meta")
    val meta: AyahMeta? = null,
    @SerializedName("surah")
    val surahInfo: SurahShort? = null
) {
    fun audioUrlFor(qoriId: String): String? {
        return audio?.get(qoriId)
            ?: audio?.get(QuranQoriCatalog.DEFAULT_ID)
            ?: audioUrl
    }
}

data class QuranQori(
    val id: String,
    val name: String,
    val audioFullFolder: String
) {
    val displayName: String
        get() = name
}

object QuranQoriCatalog {
    const val DEFAULT_ID = "05"

    val allQori = listOf(
        QuranQori("01", "Abdullah Al-Juhany", "Abdullah-Al-Juhany"),
        QuranQori("02", "Abdul Muhsin Al-Qasim", "Abdul-Muhsin-Al-Qasim"),
        QuranQori("03", "Abdurrahman As-Sudais", "Abdurrahman-as-Sudais"),
        QuranQori("04", "Ibrahim Al-Dossari", "Ibrahim-Al-Dossari"),
        QuranQori("05", "Misyari Rasyid Al-Afasi", "Misyari-Rasyid-Al-Afasi"),
        QuranQori("06", "Yasser Al-Dosari", "Yasser-Al-Dosari")
    )

    val defaultQori: QuranQori = allQori.first { it.id == DEFAULT_ID }

    fun fromId(id: String): QuranQori {
        return allQori.firstOrNull { it.id == id } ?: QuranQori(id, "Qori $id", "")
    }

    fun fromKeys(keys: Set<String>): List<QuranQori> {
        val resolved = keys
            .filter { it.isNotBlank() }
            .map { fromId(it) }
            .distinctBy { it.id }
            .sortedWith(compareBy<QuranQori> { it.id.toIntOrNull() ?: Int.MAX_VALUE }.thenBy { it.id })

        return if (resolved.isEmpty()) listOf(defaultQori) else resolved
    }

    fun audioFullUrl(qoriId: String, surahNumber: Int): String? {
        val qori = fromId(qoriId)
        if (qori.audioFullFolder.isBlank()) return null
        return "https://cdn.equran.id/audio-full/${qori.audioFullFolder}/${surahNumber.toString().padStart(3, '0')}.mp3"
    }
}

data class Tafsir(
    @SerializedName("kemenag")
    val kemenag: TafsirDetail? = null,
    @SerializedName("quraish")
    val quraish: String? = null,
    @SerializedName("jalalayn")
    val jalalayn: String? = null
)

data class TafsirDetail(
    @SerializedName("short")
    val short: String? = null,
    @SerializedName("long")
    val long: String? = null
)

data class AyahMeta(
    @SerializedName("juz")
    val juz: Int? = null,
    @SerializedName("page")
    val page: Int? = null,
    @SerializedName("manzil")
    val manzil: Int? = null,
    @SerializedName("ruku")
    val ruku: Int? = null,
    @SerializedName("hizb_quarter")
    val hizbQuarter: Int? = null,
    @SerializedName("sajda")
    val sajda: Sajda? = null
)

data class Sajda(
    @SerializedName("recommended")
    val recommended: Boolean = false,
    @SerializedName("obligatory")
    val obligatory: Boolean = false
)

data class SurahShort(
    @SerializedName("number")
    val number: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("name_latin")
    val nameLatin: String,
    @SerializedName("number_of_ayahs")
    val numberOfAyahs: Int,
    @SerializedName("translation")
    val translation: String,
    @SerializedName("revelation")
    val revelation: String
)

// ─────────────────────────────────────────────
// Responses
// ─────────────────────────────────────────────

data class SurahListResponse(
    @SerializedName("status")
    val status: Boolean,
    @SerializedName("data")
    val data: List<SurahListItem> = emptyList()
)

data class SurahListItem(
    @SerializedName("number")
    val nomor: Int,
    @SerializedName("name")
    val nama: String,
    @SerializedName("name_latin")
    val nameLatin: String,
    @SerializedName("number_of_ayahs")
    val jumlahAyat: Int,
    @SerializedName("translation")
    val arti: String,
    @SerializedName("revelation")
    val tempatTurun: String,
    @SerializedName("description")
    val deskripsi: String? = null,
    @SerializedName("audio_url")
    val audioUrl: String? = null,
    @SerializedName("audioFull")
    val audioFull: Map<String, String>? = null
)

data class SurahDetailResponse(
    @SerializedName("status")
    val status: Boolean,
    @SerializedName("data")
    val data: SurahDetail? = null,
    @SerializedName("pagination")
    val pagination: Pagination? = null
)

data class SurahDetail(
    @SerializedName("number")
    val nomor: Int,
    @SerializedName("name")
    val nama: String,
    @SerializedName("name_latin")
    val nameLatin: String,
    @SerializedName("number_of_ayahs")
    val jumlahAyat: Int,
    @SerializedName("translation")
    val arti: String,
    @SerializedName("revelation")
    val tempatTurun: String,
    @SerializedName("description")
    val deskripsi: String? = null,
    @SerializedName("audio_url")
    val audioUrl: String? = null,
    @SerializedName("audioFull")
    val audioFull: Map<String, String>? = null,
    @SerializedName("ayahs")
    val ayahs: List<Ayah> = emptyList()
) {
    fun audioUrlFor(qoriId: String): String? {
        return audioFull?.get(qoriId)
            ?: audioFull?.get(QuranQoriCatalog.DEFAULT_ID)
            ?: QuranQoriCatalog.audioFullUrl(qoriId, nomor)
            ?: audioUrl
    }

    fun availableQori(): List<QuranQori> {
        val keys = buildSet {
            audioFull?.keys?.let(::addAll)
            ayahs.forEach { ayah -> ayah.audio?.keys?.let(::addAll) }
        }
        return if (keys.isEmpty()) QuranQoriCatalog.allQori else QuranQoriCatalog.fromKeys(keys)
    }
}

data class JuzResponse(
    @SerializedName("status")
    val status: Boolean,
    @SerializedName("data")
    val data: List<Ayah> = emptyList(),
    @SerializedName("pagination")
    val pagination: Pagination? = null
)

data class AyahDetailResponse(
    @SerializedName("status")
    val status: Boolean,
    @SerializedName("data")
    val data: Ayah? = null
)

data class Pagination(
    @SerializedName("page")
    val page: Int,
    @SerializedName("limit")
    val limit: Int,
    @SerializedName("total")
    val total: Int
)

// UI Models
data class JuzDetail(
    val juz: Int,
    val startSurahNama: String,
    val endSurahNama: String,
    val ayat: List<Ayah> = emptyList()
)

data class TafsirItem(
    @SerializedName("nomor")
    val nomor: Int,
    @SerializedName("nama")
    val nama: String,
    @SerializedName("tafsir")
    val tafsir: List<TafsirContent> = emptyList()
)

data class TafsirContent(
    @SerializedName("ayat")
    val ayat: Int,
    @SerializedName("teks")
    val teks: String
)
