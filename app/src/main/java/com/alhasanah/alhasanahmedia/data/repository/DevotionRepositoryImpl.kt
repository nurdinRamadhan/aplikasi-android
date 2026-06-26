package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.BuildConfig
import com.alhasanah.alhasanahmedia.data.model.devotion.DevotionCategory
import com.alhasanah.alhasanahmedia.data.model.devotion.DevotionItem
import com.alhasanah.alhasanahmedia.data.model.devotion.DevotionLibraryData
import com.alhasanah.alhasanahmedia.data.model.devotion.KitabBook
import com.alhasanah.alhasanahmedia.data.model.devotion.KitabChapter
import com.alhasanah.alhasanahmedia.data.remote.devotion.AhmadSanusiApiService
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class DevotionRepositoryImpl(
    private val apiService: AhmadSanusiApiService,
    private val cacheStore: OfflineFirstCacheStore
) : DevotionRepository {

    override fun getDevotions(): Flow<List<DevotionItem>> = flowOf(seedDevotions)

    override fun getLibrary(): Flow<Result<OfflineFirstResource<DevotionLibraryData>>> = flow {
        val cached = cacheStore.getDevotionLibrary()
        cached?.let {
            emit(Result.success(OfflineFirstResource(it.value, isFromCache = true, updatedAt = it.updatedAt)))
        }

        val apiKey = BuildConfig.AHMAD_SANUSI_API_KEY.trim()
        if (apiKey.isBlank()) {
            val fallback = cached?.value ?: seedLibrary
            emit(
                Result.success(
                    OfflineFirstResource(
                        data = fallback,
                        isFromCache = cached != null,
                        updatedAt = cached?.updatedAt,
                        notice = "API key Ahmad Sanusi belum diatur. Menampilkan data bawaan/offline."
                    )
                )
            )
            return@flow
        }

        try {
            val categories = apiService.getDevotionCategories(apiKey).extractArray("kategori", "categories", "data")
                .mapIndexed { index, element -> element.toDevotionCategory(index) }
                .ifEmpty { seedCategories }

            val devotions = categories.flatMap { category ->
                fetchAllDevotions(apiKey, category)
            }.distinctBy { it.id.ifBlank { "${it.category}:${it.title}:${it.arabic}" } }

            val kitabCategories = apiService.getKitabCategories(apiKey).extractStringList()
            val kitabBooks = apiService.getKitabBooks(apiKey).extractArray("kitab", "books", "data")
                .mapNotNull { it.asObjectOrNull()?.toKitabBook() }

            val data = DevotionLibraryData(
                categories = categories,
                devotions = devotions.ifEmpty { seedDevotions },
                kitabCategories = kitabCategories,
                kitabBooks = kitabBooks
            )
            cacheStore.saveDevotionLibrary(data)
            emit(Result.success(OfflineFirstResource(data, isFromCache = false)))
        } catch (e: Exception) {
            val fallback = cached?.value ?: seedLibrary
            emit(
                Result.success(
                    OfflineFirstResource(
                        data = fallback,
                        isFromCache = true,
                        updatedAt = cached?.updatedAt,
                        notice = e.message ?: "Mode offline. Menampilkan data tersimpan."
                    )
                )
            )
        }
    }

    override fun getKitabChapters(slug: String): Flow<Result<OfflineFirstResource<List<KitabChapter>>>> = flow {
        val cached = cacheStore.getKitabChapters(slug)
        cached?.let {
            emit(Result.success(OfflineFirstResource(it.value, isFromCache = true, updatedAt = it.updatedAt)))
        }

        val apiKey = BuildConfig.AHMAD_SANUSI_API_KEY.trim()
        if (apiKey.isBlank()) {
            if (cached == null) emit(Result.failure(Exception("API key Ahmad Sanusi belum diatur.")))
            return@flow
        }

        try {
            val chapters = fetchAllKitabChapters(apiKey, slug)
            cacheStore.saveKitabChapters(slug, chapters)
            emit(Result.success(OfflineFirstResource(chapters, isFromCache = false)))
        } catch (e: Exception) {
            if (cached == null) {
                emit(Result.failure(e))
            } else {
                emit(Result.success(OfflineFirstResource(cached.value, isFromCache = true, updatedAt = cached.updatedAt, notice = "Mode offline. Menampilkan bab tersimpan.")))
            }
        }
    }

    override fun getKitabChapterDetail(slug: String, number: Int): Flow<Result<OfflineFirstResource<KitabChapter>>> = flow {
        val cached = cacheStore.getKitabChapterDetail(slug, number)
        cached?.let {
            emit(Result.success(OfflineFirstResource(it.value, isFromCache = true, updatedAt = it.updatedAt)))
        }

        val apiKey = BuildConfig.AHMAD_SANUSI_API_KEY.trim()
        if (apiKey.isBlank()) {
            if (cached == null) emit(Result.failure(Exception("API key Ahmad Sanusi belum diatur.")))
            return@flow
        }

        try {
            val detail = apiService.getKitabChapterDetail(apiKey, slug, number)
                .extractObject("bab", "chapter", "data")
                ?.toKitabChapter(slug)
                ?: throw IllegalStateException("Bab kitab tidak ditemukan.")
            cacheStore.saveKitabChapterDetail(slug, detail)
            emit(Result.success(OfflineFirstResource(detail, isFromCache = false)))
        } catch (e: Exception) {
            if (cached == null) emit(Result.failure(e))
        }
    }

    private suspend fun fetchAllDevotions(apiKey: String, category: DevotionCategory): List<DevotionItem> {
        val items = mutableListOf<DevotionItem>()
        var page = 1
        val limit = 100
        do {
            val response = apiService.getDevotionsByCategory(apiKey, category.slug, page, limit)
            val data = response.extractObject("data")
            val pageItems = response.extractArray("doa", "items", "data").mapIndexed { index, element ->
                element.toDevotionItem(category, page, index)
            }
            items += pageItems
            val total = data?.intOrNull("total", "total_data") ?: response.intOrNull("total", "total_data") ?: category.total
            page += 1
        } while (pageItems.size == limit && (total == 0 || items.size < total) && page <= 10)
        return items
    }

    private suspend fun fetchAllKitabChapters(apiKey: String, slug: String): List<KitabChapter> {
        val chapters = mutableListOf<KitabChapter>()
        var page = 1
        val limit = 200
        do {
            val response = apiService.getKitabChapters(apiKey, slug, page, limit)
            val pageItems = response.extractArray("bab", "chapters", "items", "data").mapNotNull {
                it.asObjectOrNull()?.toKitabChapter(slug)
            }
            chapters += pageItems
            val total = response.extractObject("data")?.intOrNull("total", "total_bab", "total_chapters")
                ?: response.intOrNull("total", "total_bab", "total_chapters")
                ?: 0
            page += 1
        } while (pageItems.size == limit && (total == 0 || chapters.size < total) && page <= 20)
        return chapters.distinctBy { it.number }
    }

    private fun JsonElement.toDevotionCategory(index: Int): DevotionCategory {
        val obj = asObjectOrNull()
        if (obj == null) {
            val label = asStringOrBlank().ifBlank { "Kategori ${index + 1}" }
            return DevotionCategory(name = label, slug = label.toSlug())
        }
        val name = obj.stringOrNull("nama", "name", "judul", "title") ?: "Kategori ${index + 1}"
        return DevotionCategory(
            name = name,
            slug = obj.stringOrNull("slug", "id") ?: name.toSlug(),
            total = obj.intOrNull("total", "jumlah", "count") ?: 0
        )
    }

    private fun JsonElement.toDevotionItem(category: DevotionCategory, page: Int, index: Int): DevotionItem {
        val obj = asObjectOrNull()
        if (obj == null) {
            return DevotionItem(
                id = "${category.slug}-$page-$index",
                title = "Bacaan ${index + 1}",
                category = category.name,
                arabic = asStringOrBlank(),
                latin = "",
                translation = ""
            )
        }
        val id = obj.stringOrNull("id", "doa_id") ?: "${category.slug}-$page-$index"
        return DevotionItem(
            id = id,
            title = obj.stringOrNull("judul", "title", "nama") ?: "Bacaan ${index + 1}",
            category = category.name,
            arabic = obj.stringOrNull("arab", "arabic", "teks_arab") ?: "",
            latin = obj.stringOrNull("latin", "transliterasi") ?: "",
            translation = obj.stringOrNull("terjemah", "terjemahan", "translation") ?: "",
            source = obj.stringOrNull("sumber", "source") ?: "",
            fawaid = obj.stringOrNull("fawaid", "faedah", "keutamaan") ?: "",
            note = obj.stringOrNull("catatan", "note", "keterangan") ?: ""
        )
    }

    private fun JsonObject.toKitabBook(): KitabBook? {
        val slug = stringOrNull("slug", "id") ?: return null
        val title = stringOrNull("nama", "judul", "title", "name") ?: slug.replace('-', ' ')
        return KitabBook(
            slug = slug,
            title = title,
            author = stringOrNull("pengarang", "author", "penulis") ?: "",
            category = stringOrNull("kategori", "category") ?: "",
            description = stringOrNull("deskripsi", "description", "keterangan") ?: "",
            totalChapters = intOrNull("total_bab", "jumlah_bab", "total_chapters", "total") ?: 0
        )
    }

    private fun JsonObject.toKitabChapter(slug: String): KitabChapter {
        val number = intOrNull("nomor", "no", "number", "bab") ?: 0
        return KitabChapter(
            id = "$slug-$number",
            bookSlug = slug,
            number = number,
            title = stringOrNull("judul", "title", "nama") ?: "Bab $number",
            arabic = stringOrNull("arab", "arabic", "teks_arab") ?: "",
            latin = stringOrNull("latin", "transliterasi") ?: "",
            translation = stringOrNull("terjemah", "terjemahan", "translation") ?: "",
            content = stringOrNull("isi", "content", "matan", "teks", "deskripsi") ?: ""
        )
    }

    private fun JsonObject.extractArray(vararg names: String): List<JsonElement> {
        names.forEach { name ->
            get(name)?.let { element ->
                when {
                    element.isJsonArray -> return element.asJsonArray.toList()
                    element.isJsonObject -> {
                        val nested = element.asJsonObject.extractArray("doa", "kitab", "bab", "items", "data", "categories")
                        if (nested.isNotEmpty()) return nested
                    }
                }
            }
        }
        return emptyList()
    }

    private fun JsonObject.extractStringList(): List<String> =
        extractArray("kategori", "categories", "data").mapNotNull {
            it.asStringOrBlank().ifBlank {
                it.asObjectOrNull()?.stringOrNull("nama", "name", "kategori", "category")
            }
        }.distinct()

    private fun JsonObject.extractObject(vararg names: String): JsonObject? {
        names.forEach { name ->
            val element = get(name)
            if (element != null && element.isJsonObject) return element.asJsonObject
        }
        return null
    }

    private fun JsonElement.asObjectOrNull(): JsonObject? =
        if (isJsonObject) asJsonObject else null

    private fun JsonElement.asStringOrBlank(): String =
        runCatching { if (isJsonPrimitive && asJsonPrimitive.isString) asString else "" }.getOrDefault("")

    private fun JsonObject.stringOrNull(vararg names: String): String? =
        names.firstNotNullOfOrNull { name ->
            get(name)?.takeIf { !it.isJsonNull }?.let { element ->
                runCatching { element.asString.trim() }.getOrNull()?.takeIf { it.isNotBlank() }
            }
        }

    private fun JsonObject.intOrNull(vararg names: String): Int? =
        names.firstNotNullOfOrNull { name ->
            get(name)?.takeIf { !it.isJsonNull }?.let { element ->
                runCatching { element.asInt }.getOrNull()
            }
        }

    private fun String.toSlug(): String =
        lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "kategori" }

    private val seedCategories = listOf(
        DevotionCategory("Dzikir Pagi", "dzikir-pagi", 1),
        DevotionCategory("Dzikir Petang", "dzikir-petang", 1),
        DevotionCategory("Doa Harian", "doa-harian", 4)
    )

    private val seedLibrary: DevotionLibraryData
        get() = DevotionLibraryData(
            categories = seedCategories,
            devotions = seedDevotions
        )

    private val seedDevotions = listOf(
        DevotionItem(
            id = "morning-1",
            title = "Dzikir Pagi",
            category = "Dzikir Pagi",
            arabic = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلّٰهِ",
            latin = "Ashbahna wa ashbahal mulku lillah",
            translation = "Kami memasuki waktu pagi dan kerajaan hanya milik Allah.",
            source = "Dzikir pagi"
        ),
        DevotionItem(
            id = "evening-1",
            title = "Dzikir Petang",
            category = "Dzikir Petang",
            arabic = "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلّٰهِ",
            latin = "Amsaina wa amsal mulku lillah",
            translation = "Kami memasuki waktu petang dan kerajaan hanya milik Allah.",
            source = "Dzikir petang"
        ),
        DevotionItem(
            id = "sleep-1",
            title = "Doa Sebelum Tidur",
            category = "Doa Harian",
            arabic = "بِاسْمِكَ اللّٰهُمَّ أَحْيَا وَأَمُوتُ",
            latin = "Bismika Allahumma ahya wa amut",
            translation = "Dengan nama-Mu ya Allah aku hidup dan aku mati.",
            source = "HR. Bukhari"
        ),
        DevotionItem(
            id = "wake-1",
            title = "Doa Bangun Tidur",
            category = "Doa Harian",
            arabic = "الْحَمْدُ لِلّٰهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ",
            latin = "Alhamdulillahil ladzi ahyana ba'da ma amatana wa ilaihin nusyur",
            translation = "Segala puji bagi Allah yang menghidupkan kami setelah mematikan kami, dan kepada-Nya kami dibangkitkan.",
            source = "HR. Bukhari"
        )
    )
}
