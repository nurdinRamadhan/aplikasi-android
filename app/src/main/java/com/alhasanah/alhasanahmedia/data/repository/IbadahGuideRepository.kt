package com.alhasanah.alhasanahmedia.data.repository

import android.content.Context
import com.alhasanah.alhasanahmedia.data.model.ibadah.IbadahChapter
import com.alhasanah.alhasanahmedia.data.model.ibadah.IbadahGuide
import com.alhasanah.alhasanahmedia.data.model.ibadah.IbadahGuideCatalog
import com.alhasanah.alhasanahmedia.data.model.ibadah.IbadahPrayer
import com.alhasanah.alhasanahmedia.data.model.ibadah.IbadahSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface IbadahGuideRepository {
    suspend fun getCatalog(): IbadahGuideCatalog
}

class IbadahGuideRepositoryImpl(
    private val context: Context
) : IbadahGuideRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private var memoryCache: IbadahGuideCatalog? = null

    override suspend fun getCatalog(): IbadahGuideCatalog = withContext(Dispatchers.IO) {
        memoryCache ?: loadCatalog().also { memoryCache = it }
    }

    private fun loadCatalog(): IbadahGuideCatalog {
        val base = context.assets.open(ASSET_NAME).bufferedReader().use { reader ->
            json.decodeFromString(IbadahGuideCatalog.serializer(), reader.readText())
        }
        val apiGuides = loadApiIslamiShalatGuides()
        val sources = base.sources + IbadahSource(
            name = "API-Islami - Muslim Api V2 by Kang Muhtar",
            url = "https://github.com/kiramizuky/API-Islami"
        )
        return base.copy(
            sources = sources.distinctBy { it.url.ifBlank { it.name } },
            guides = (base.guides + apiGuides).distinctBy { it.id }
        )
    }

    private fun loadApiIslamiShalatGuides(): List<IbadahGuide> {
        val niat = readJsonObjectOrNull(API_SHALAT_NIAT)
            ?.get("data")
            ?.jsonArrayOrNull()
            .orEmpty()
            .mapNotNull { it.jsonObjectOrNull()?.toPrayer() }

        val bacaan = readJsonArrayOrNull(API_SHALAT_BACAAN)
            .orEmpty()
            .mapNotNull { item ->
                val obj = item.jsonObjectOrNull() ?: return@mapNotNull null
                val title = obj.string("nama").ifBlank { "Bacaan Shalat" }
                IbadahChapter(
                    id = "api-islami-bacaan-${title.slug()}",
                    title = title,
                    description = "Bacaan yang bersumber dari data API-Islami dan disimpan offline di aplikasi.",
                    prayers = obj["bacaan"]?.jsonArrayOrNull().orEmpty().mapNotNull { it.jsonObjectOrNull()?.toPrayer(title) }
                )
            }

        val dzikir = readJsonObjectOrNull(API_SHALAT_DZIKIR)
            ?.get("doa")
            ?.jsonArrayOrNull()
            .orEmpty()
            .mapNotNull { it.jsonObjectOrNull()?.toPrayer() }

        return listOf(
            IbadahGuide(
                id = "api-islami-niat-shalat",
                title = "Niat Shalat Wajib",
                category = "Shalat",
                summary = "Bacaan niat shalat wajib dari API-Islami, tersedia penuh offline.",
                icon = "prayer",
                chapters = listOf(
                    IbadahChapter(
                        id = "api-islami-niat-shalat-wajib",
                        title = "Niat Shalat Wajib",
                        description = "Kumpulan lafaz niat shalat Subuh, Dzuhur, Ashar, Maghrib, dan Isya.",
                        prayers = niat
                    )
                )
            ),
            IbadahGuide(
                id = "api-islami-bacaan-shalat",
                title = "Bacaan Shalat Lengkap",
                category = "Shalat",
                summary = "Bacaan dalam gerakan shalat dari takbiratul ihram sampai salam, disimpan offline.",
                icon = "prayer",
                chapters = bacaan
            ),
            IbadahGuide(
                id = "api-islami-dzikir-setelah-shalat",
                title = "Dzikir Setelah Shalat",
                category = "Shalat",
                summary = "Dzikir dan doa setelah shalat dari API-Islami, tersedia offline.",
                icon = "prayer",
                chapters = listOf(
                    IbadahChapter(
                        id = "api-islami-dzikir-setelah-shalat",
                        title = "Dzikir Setelah Shalat",
                        description = "Bacaan dzikir setelah shalat wajib beserta latin, arti, catatan, dan sumber jika tersedia.",
                        prayers = dzikir
                    )
                )
            )
        ) + loadApiIslamiTahlilGuide() + loadApiIslamiDoaSourceGuides()
    }

    private fun loadApiIslamiTahlilGuide(): List<IbadahGuide> {
        val prayers = readJsonObjectOrNull(API_TAHLIL)
            ?.get("data")
            ?.jsonArrayOrNull()
            .orEmpty()
            .mapNotNull { item ->
                val obj = item.jsonObjectOrNull() ?: return@mapNotNull null
                IbadahPrayer(
                    title = obj.string("judul").ifBlank { "Bacaan Tahlil" },
                    arabic = obj.string("arab"),
                    translation = obj.string("id")
                )
            }
        if (prayers.isEmpty()) return emptyList()
        return listOf(
            IbadahGuide(
                id = "api-islami-tahlil",
                title = "Bacaan Tahlil",
                category = "Doa & Dzikir",
                summary = "Rangkaian bacaan tahlil dari API-Islami, disimpan offline di aplikasi.",
                icon = "prayer",
                chapters = listOf(
                    IbadahChapter(
                        id = "api-islami-rangkaian-tahlil",
                        title = "Rangkaian Tahlil",
                        description = "Bacaan tahlil berurutan dengan teks Arab dan terjemahan.",
                        prayers = prayers
                    )
                )
            )
        )
    }

    private fun loadApiIslamiDoaSourceGuides(): List<IbadahGuide> {
        val hajiPrayers = readDoaSourcePrayers(API_DOA_HAJI)
        val ibadahPrayers = readDoaSourcePrayers(API_DOA_IBADAH)
        return listOfNotNull(
            hajiPrayers.takeIf { it.isNotEmpty() }?.let {
                IbadahGuide(
                    id = "api-islami-doa-haji-umrah",
                    title = "Doa Haji dan Umrah",
                    category = "Haji & Umrah",
                    summary = "Doa perjalanan haji dan umrah seperti masuk Makkah, thawaf, sa'i, ihram, dan wukuf.",
                    icon = "kaaba",
                    chapters = listOf(
                        IbadahChapter(
                            id = "api-islami-doa-manasik-haji-umrah",
                            title = "Doa Manasik",
                            description = "Kumpulan doa haji dan umrah dari sumber API-Islami, tersedia offline.",
                            prayers = it
                        )
                    )
                )
            },
            ibadahPrayers.takeIf { it.isNotEmpty() }?.let {
                IbadahGuide(
                    id = "api-islami-doa-ibadah",
                    title = "Doa Ibadah Pilihan",
                    category = "Doa & Dzikir",
                    summary = "Doa setelah wudhu, adzan, Dhuha, Istikharah, Tahajud, dan doa ibadah lain.",
                    icon = "prayer",
                    chapters = listOf(
                        IbadahChapter(
                            id = "api-islami-doa-ibadah-pilihan",
                            title = "Doa Ibadah",
                            description = "Kumpulan doa ibadah yang disimpan offline dari API-Islami.",
                            prayers = it
                        )
                    )
                )
            }
        )
    }

    private fun readDoaSourcePrayers(assetName: String): List<IbadahPrayer> {
        val root = readJsonObjectOrNull(assetName) ?: return emptyList()
        return root["data"]
            ?.jsonObjectOrNull()
            ?.get("data")
            ?.jsonArrayOrNull()
            .orEmpty()
            .mapNotNull { item ->
                val obj = item.jsonObjectOrNull() ?: return@mapNotNull null
                IbadahPrayer(
                    title = obj.string("judul").ifBlank { "Doa" },
                    arabic = obj.string("doa", "arab", "arabic"),
                    latin = obj.string("latin"),
                    translation = obj.string("artinya", "arti", "translation")
                )
            }
    }

    private fun JsonObject.toPrayer(fallbackTitle: String = ""): IbadahPrayer {
        val note = string("notes", "note", "catatan")
        val source = string("source", "sumber")
        val fawaid = string("fawaid", "faedah")
        val translation = listOf(
            string("arti", "translation", "terjemah"),
            note.takeIf { it.isNotBlank() }?.let { "Catatan: $it" }.orEmpty(),
            fawaid.takeIf { it.isNotBlank() }?.let { "Fawaid: $it" }.orEmpty(),
            source.takeIf { it.isNotBlank() }?.let { "Sumber: $it" }.orEmpty()
        ).filter { it.isNotBlank() }.joinToString("\n\n")
        return IbadahPrayer(
            title = string("nama", "title").ifBlank { fallbackTitle.ifBlank { "Bacaan" } },
            arabic = string("arab", "arabic"),
            latin = string("latin"),
            translation = translation
        )
    }

    private fun readJsonObjectOrNull(assetName: String): JsonObject? =
        runCatching {
            json.parseToJsonElement(
                context.assets.open(assetName).bufferedReader().use { it.readText() }
            ).jsonObject
        }.getOrNull()

    private fun readJsonArrayOrNull(assetName: String): JsonArray? =
        runCatching {
            json.parseToJsonElement(
                context.assets.open(assetName).bufferedReader().use { it.readText() }
            ).jsonArray
        }.getOrNull()

    private fun JsonElement.jsonObjectOrNull(): JsonObject? =
        runCatching { jsonObject }.getOrNull()

    private fun JsonElement.jsonArrayOrNull(): JsonArray? =
        runCatching { jsonArray }.getOrNull()

    private fun JsonObject.string(vararg names: String): String =
        names.firstNotNullOfOrNull { name ->
            get(name)?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.let {
                runCatching { it.jsonPrimitive.content.trim() }.getOrNull()
            }
        }.orEmpty()

    private fun String.slug(): String =
        lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "bacaan" }

    private companion object {
        const val ASSET_NAME = "ibadah_guides.json"
        const val API_SHALAT_NIAT = "api_islami_shalat_niat.json"
        const val API_SHALAT_BACAAN = "api_islami_shalat_bacaan.json"
        const val API_SHALAT_DZIKIR = "api_islami_shalat_dzikir.json"
        const val API_TAHLIL = "api_islami_tahlil.json"
        const val API_DOA_HAJI = "api_islami_doa_haji.json"
        const val API_DOA_IBADAH = "api_islami_doa_ibadah.json"
    }
}
