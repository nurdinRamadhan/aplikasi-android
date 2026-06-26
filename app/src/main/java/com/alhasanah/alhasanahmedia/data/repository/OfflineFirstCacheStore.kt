package com.alhasanah.alhasanahmedia.data.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alhasanah.alhasanahmedia.data.local.AlumniCacheDao
import com.alhasanah.alhasanahmedia.data.local.AlumniCacheEntity
import com.alhasanah.alhasanahmedia.data.model.AbsensiHarianItem
import com.alhasanah.alhasanahmedia.data.model.HafalanKitab
import com.alhasanah.alhasanahmedia.data.model.HafalanTahfidz
import com.alhasanah.alhasanahmedia.data.model.KesehatanSantri
import com.alhasanah.alhasanahmedia.data.model.MurojaahTahfidz
import com.alhasanah.alhasanahmedia.data.model.PelanggaranSantri
import com.alhasanah.alhasanahmedia.data.model.PerizinanSantri
import com.alhasanah.alhasanahmedia.data.model.RingkasanAbsensiMingguan
import com.alhasanah.alhasanahmedia.data.model.SantriModel
import com.alhasanah.alhasanahmedia.data.model.TagihanWithDetail
import com.alhasanah.alhasanahmedia.data.model.devotion.DevotionLibraryData
import com.alhasanah.alhasanahmedia.data.model.devotion.KitabChapter
import com.alhasanah.alhasanahmedia.data.model.hadith.HadithExploreData
import com.alhasanah.alhasanahmedia.data.model.hadith.HadithItem
import com.alhasanah.alhasanahmedia.data.model.hadith.HadithSearchData
import com.alhasanah.alhasanahmedia.data.model.islamiccalendar.CalendarData
import com.alhasanah.alhasanahmedia.data.model.islamiccalendar.HolidayItem
import com.alhasanah.alhasanahmedia.data.model.prayer.PrayerLocation
import com.alhasanah.alhasanahmedia.data.model.prayer.PrayerScheduleData
import java.security.MessageDigest
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

data class CachedValue<T>(
    val value: T,
    val updatedAt: Long
)

private val Context.offlineFirstDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "offline_first_cache"
)

class OfflineFirstCacheStore(
    private val context: Context,
    private val dao: AlumniCacheDao
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val cipher = AndroidKeystoreJsonCipher()

    suspend fun getSantriList(userId: String): List<SantriModel>? =
        readList(key("santri_list", userId), SantriModel.serializer())

    suspend fun saveSantriList(userId: String, items: List<SantriModel>) {
        writeList(key("santri_list", userId), "santri_summary", SantriModel.serializer(), items)
    }

    suspend fun getSantriDetail(userId: String, nis: String): SantriModel? =
        readEncrypted(key("santri_detail", "$userId:$nis"), SantriModel.serializer())

    suspend fun saveSantriDetail(userId: String, item: SantriModel) {
        writeEncrypted(key("santri_detail", "$userId:${item.id}"), "santri_sensitive_detail", SantriModel.serializer(), item)
    }

    suspend fun getHafalanTahfidz(nis: String): List<HafalanTahfidz>? =
        readList(key("hafalan_tahfidz", nis), HafalanTahfidz.serializer())

    suspend fun saveHafalanTahfidz(nis: String, items: List<HafalanTahfidz>) {
        writeList(key("hafalan_tahfidz", nis), "hafalan_tahfidz", HafalanTahfidz.serializer(), items)
    }

    suspend fun getMurojaahTahfidz(nis: String): List<MurojaahTahfidz>? =
        readList(key("murojaah_tahfidz", nis), MurojaahTahfidz.serializer())

    suspend fun saveMurojaahTahfidz(nis: String, items: List<MurojaahTahfidz>) {
        writeList(key("murojaah_tahfidz", nis), "murojaah_tahfidz", MurojaahTahfidz.serializer(), items)
    }

    suspend fun getHafalanKitab(nis: String): List<HafalanKitab>? =
        readList(key("hafalan_kitab", nis), HafalanKitab.serializer())

    suspend fun saveHafalanKitab(nis: String, items: List<HafalanKitab>) {
        writeList(key("hafalan_kitab", nis), "hafalan_kitab", HafalanKitab.serializer(), items)
    }

    suspend fun getPelanggaran(nis: String): List<PelanggaranSantri>? =
        readList(key("pelanggaran", nis), PelanggaranSantri.serializer())

    suspend fun savePelanggaran(nis: String, items: List<PelanggaranSantri>) {
        writeList(key("pelanggaran", nis), "pelanggaran", PelanggaranSantri.serializer(), items)
    }

    suspend fun getPerizinan(nis: String): List<PerizinanSantri>? =
        readList(key("perizinan", nis), PerizinanSantri.serializer())

    suspend fun savePerizinan(nis: String, items: List<PerizinanSantri>) {
        writeList(key("perizinan", nis), "perizinan", PerizinanSantri.serializer(), items)
    }

    suspend fun getKesehatan(nis: String): List<KesehatanSantri>? =
        readList(key("kesehatan", nis), KesehatanSantri.serializer())

    suspend fun saveKesehatan(nis: String, items: List<KesehatanSantri>) {
        writeList(key("kesehatan", nis), "kesehatan", KesehatanSantri.serializer(), items)
    }

    suspend fun getRingkasanAbsensiMingguan(nis: String, weekStart: String): RingkasanAbsensiMingguan? =
        readCached(key("ringkasan_absensi_mingguan", "${nis}_${weekStart}"), RingkasanAbsensiMingguan.serializer())?.value

    suspend fun saveRingkasanAbsensiMingguan(nis: String, weekStart: String, data: RingkasanAbsensiMingguan) {
        write(
            key = key("ringkasan_absensi_mingguan", "${nis}_${weekStart}"),
            domain = "ringkasan_absensi_mingguan",
            serializer = RingkasanAbsensiMingguan.serializer(),
            value = data,
            expiresAt = fromNow(AbsensiCacheTTL)
        )
    }

    suspend fun getTagihan(nis: String): List<TagihanWithDetail>? =
        readList(key("tagihan", nis), TagihanWithDetail.serializer())

    suspend fun saveTagihan(nis: String, items: List<TagihanWithDetail>) {
        writeList(key("tagihan", nis), "tagihan", TagihanWithDetail.serializer(), items)
    }

    suspend fun getPrayerLocations(keyword: String): CachedValue<List<PrayerLocation>>? =
        readListCached(key("prayer_locations", keyword.normalizedCacheKey()), PrayerLocation.serializer())

    suspend fun savePrayerLocations(keyword: String, items: List<PrayerLocation>) {
        writeList(
            key = key("prayer_locations", keyword.normalizedCacheKey()),
            domain = "prayer_locations",
            serializer = PrayerLocation.serializer(),
            value = items,
            expiresAt = fromNow(LOCATION_TTL_MILLIS)
        )
    }

    suspend fun getPrayerTodaySchedule(
        locationId: String,
        timezone: String,
        dateKey: String
    ): CachedValue<PrayerScheduleData>? =
        readCached(key("prayer_today", "$locationId:$timezone:$dateKey"), PrayerScheduleData.serializer())

    suspend fun savePrayerTodaySchedule(
        locationId: String,
        timezone: String,
        dateKey: String,
        data: PrayerScheduleData
    ) {
        write(
            key = key("prayer_today", "$locationId:$timezone:$dateKey"),
            domain = "prayer_today",
            serializer = PrayerScheduleData.serializer(),
            value = data,
            expiresAt = fromNow(PRAYER_SCHEDULE_TTL_MILLIS)
        )
    }

    suspend fun getPrayerMonthlySchedule(
        locationId: String,
        yearMonth: String
    ): CachedValue<PrayerScheduleData>? =
        readCached(key("prayer_monthly", "$locationId:$yearMonth"), PrayerScheduleData.serializer())

    suspend fun savePrayerMonthlySchedule(
        locationId: String,
        yearMonth: String,
        data: PrayerScheduleData
    ) {
        write(
            key = key("prayer_monthly", "$locationId:$yearMonth"),
            domain = "prayer_monthly",
            serializer = PrayerScheduleData.serializer(),
            value = data,
            expiresAt = fromNow(PRAYER_MONTHLY_SCHEDULE_TTL_MILLIS)
        )
    }

    suspend fun getHadithExplore(page: Int, limit: Int): CachedValue<HadithExploreData>? =
        readCached(key("hadith_explore", "$page:$limit"), HadithExploreData.serializer())

    suspend fun saveHadithExplore(page: Int, limit: Int, data: HadithExploreData) {
        write(
            key = key("hadith_explore", "$page:$limit"),
            domain = "hadith_explore",
            serializer = HadithExploreData.serializer(),
            value = data,
            expiresAt = fromNow(HADITH_LIST_TTL_MILLIS)
        )
    }

    suspend fun getHadithSearch(keyword: String, page: Int, limit: Int): CachedValue<HadithSearchData>? =
        readCached(key("hadith_search", "${keyword.normalizedCacheKey()}:$page:$limit"), HadithSearchData.serializer())

    suspend fun saveHadithSearch(keyword: String, page: Int, limit: Int, data: HadithSearchData) {
        write(
            key = key("hadith_search", "${keyword.normalizedCacheKey()}:$page:$limit"),
            domain = "hadith_search",
            serializer = HadithSearchData.serializer(),
            value = data,
            expiresAt = fromNow(HADITH_LIST_TTL_MILLIS)
        )
    }

    suspend fun getHadithDetail(id: Int): CachedValue<HadithItem>? =
        readCached(key("hadith_detail", id.toString()), HadithItem.serializer())

    suspend fun saveHadithDetail(item: HadithItem) {
        write(
            key = key("hadith_detail", item.id.toString()),
            domain = "hadith_detail",
            serializer = HadithItem.serializer(),
            value = item,
            expiresAt = fromNow(HADITH_DETAIL_TTL_MILLIS)
        )
    }

    suspend fun getDevotionLibrary(): CachedValue<DevotionLibraryData>? =
        readCached(key("devotion_library", "ahmad_sanusi"), DevotionLibraryData.serializer())

    suspend fun saveDevotionLibrary(data: DevotionLibraryData) {
        write(
            key = key("devotion_library", "ahmad_sanusi"),
            domain = "devotion_library",
            serializer = DevotionLibraryData.serializer(),
            value = data,
            expiresAt = fromNow(DEVOTION_LIBRARY_TTL_MILLIS)
        )
    }

    suspend fun getKitabChapters(slug: String): CachedValue<List<KitabChapter>>? =
        readListCached(key("kitab_chapters", slug.normalizedCacheKey()), KitabChapter.serializer())

    suspend fun saveKitabChapters(slug: String, items: List<KitabChapter>) {
        writeList(
            key = key("kitab_chapters", slug.normalizedCacheKey()),
            domain = "kitab_chapters",
            serializer = KitabChapter.serializer(),
            value = items,
            expiresAt = fromNow(KITAB_CHAPTER_TTL_MILLIS)
        )
    }

    suspend fun getKitabChapterDetail(slug: String, number: Int): CachedValue<KitabChapter>? =
        readCached(key("kitab_chapter_detail", "${slug.normalizedCacheKey()}:$number"), KitabChapter.serializer())

    suspend fun saveKitabChapterDetail(slug: String, item: KitabChapter) {
        write(
            key = key("kitab_chapter_detail", "${slug.normalizedCacheKey()}:${item.number}"),
            domain = "kitab_chapter_detail",
            serializer = KitabChapter.serializer(),
            value = item,
            expiresAt = fromNow(KITAB_CHAPTER_TTL_MILLIS)
        )
    }

    suspend fun getIslamicCalendarToday(
        timezone: String,
        method: String,
        adjustment: Int,
        dateKey: String
    ): CachedValue<CalendarData>? =
        readCached(key("islamic_calendar_today", "$timezone:$method:$adjustment:$dateKey"), CalendarData.serializer())

    suspend fun saveIslamicCalendarToday(
        timezone: String,
        method: String,
        adjustment: Int,
        dateKey: String,
        data: CalendarData
    ) {
        write(
            key = key("islamic_calendar_today", "$timezone:$method:$adjustment:$dateKey"),
            domain = "islamic_calendar_today",
            serializer = CalendarData.serializer(),
            value = data,
            expiresAt = fromNow(CALENDAR_TTL_MILLIS)
        )
    }

    suspend fun getHolidayList(year: Int): CachedValue<List<HolidayItem>>? =
        readListCached(key("holiday_list", year.toString()), HolidayItem.serializer())

    suspend fun saveHolidayList(year: Int, items: List<HolidayItem>) {
        writeList(
            key = key("holiday_list", year.toString()),
            domain = "holiday_list",
            serializer = HolidayItem.serializer(),
            value = items,
            expiresAt = fromNow(HOLIDAY_TTL_MILLIS)
        )
    }

    suspend fun clearSensitiveSantriDetails() {
        dao.deleteDomain("santri_sensitive_detail")
    }

    private suspend fun <T> read(key: String, serializer: KSerializer<T>): T? {
        val entity = dao.get(key) ?: return null
        if (isExpired(entity)) return null
        return runCatching { json.decodeFromString(serializer, entity.json) }.getOrNull()
    }

    private suspend fun <T> readCached(key: String, serializer: KSerializer<T>): CachedValue<T>? {
        val entity = dao.get(key) ?: return null
        if (isExpired(entity)) return null
        return runCatching {
            CachedValue(json.decodeFromString(serializer, entity.json), entity.updatedAt)
        }.getOrNull()
    }

    private suspend fun <T> readEncrypted(key: String, serializer: KSerializer<T>): T? {
        val entity = dao.get(key) ?: return null
        if (isExpired(entity)) return null
        return runCatching {
            val decrypted = cipher.decrypt(entity.json)
            json.decodeFromString(serializer, decrypted)
        }.getOrNull()
    }

    private suspend fun <T> write(
        key: String,
        domain: String,
        serializer: KSerializer<T>,
        value: T,
        expiresAt: Long? = null
    ) {
        persist(key, domain, json.encodeToString(serializer, value), expiresAt)
    }

    private suspend fun <T> writeEncrypted(
        key: String,
        domain: String,
        serializer: KSerializer<T>,
        value: T
    ) {
        persist(key, domain, cipher.encrypt(json.encodeToString(serializer, value)))
    }

    private suspend fun <T> readList(key: String, serializer: KSerializer<T>): List<T>? {
        val entity = dao.get(key) ?: return null
        if (isExpired(entity)) return null
        return runCatching { json.decodeFromString(ListSerializer(serializer), entity.json) }.getOrNull()
    }

    private suspend fun <T> writeList(
        key: String,
        domain: String,
        serializer: KSerializer<T>,
        value: List<T>,
        expiresAt: Long? = null
    ) {
        persist(key, domain, json.encodeToString(ListSerializer(serializer), value), expiresAt)
    }

    private suspend fun <T> readListCached(key: String, serializer: KSerializer<T>): CachedValue<List<T>>? {
        val entity = dao.get(key) ?: return null
        if (isExpired(entity)) return null
        return runCatching {
            CachedValue(json.decodeFromString(ListSerializer(serializer), entity.json), entity.updatedAt)
        }.getOrNull()
    }

    private suspend fun persist(key: String, domain: String, value: String, expiresAt: Long? = null) {
        val now = System.currentTimeMillis()
        dao.upsert(
            AlumniCacheEntity(
                cacheKey = key,
                domain = domain,
                json = value,
                updatedAt = now,
                expiresAt = expiresAt
            )
        )
        val stampKey = longPreferencesKey("synced_at_$key")
        context.offlineFirstDataStore.edit { it[stampKey] = now }
        dao.deleteExpired(now)
    }

    private suspend fun isExpired(entity: AlumniCacheEntity): Boolean {
        val expiresAt = entity.expiresAt
        if (expiresAt != null && expiresAt < System.currentTimeMillis()) {
            dao.delete(entity.cacheKey)
            return true
        }
        return false
    }

    private fun key(prefix: String, id: String): String =
        "${prefix}_${id.sha256()}"

    private fun fromNow(durationMillis: Long): Long =
        System.currentTimeMillis() + durationMillis

    private fun String.normalizedCacheKey(): String =
        trim().lowercase().replace(Regex("\\s+"), " ")

    private fun String.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }

    private companion object {
        val LOCATION_TTL_MILLIS: Long = TimeUnit.DAYS.toMillis(30)
        val PRAYER_SCHEDULE_TTL_MILLIS: Long = TimeUnit.HOURS.toMillis(48)
        val PRAYER_MONTHLY_SCHEDULE_TTL_MILLIS: Long = TimeUnit.DAYS.toMillis(40)
        val HADITH_LIST_TTL_MILLIS: Long = TimeUnit.DAYS.toMillis(90)
        val HADITH_DETAIL_TTL_MILLIS: Long = TimeUnit.DAYS.toMillis(180)
        val DEVOTION_LIBRARY_TTL_MILLIS: Long = TimeUnit.DAYS.toMillis(90)
        val KITAB_CHAPTER_TTL_MILLIS: Long = TimeUnit.DAYS.toMillis(180)
        val CALENDAR_TTL_MILLIS: Long = TimeUnit.HOURS.toMillis(36)
        val HOLIDAY_TTL_MILLIS: Long = TimeUnit.DAYS.toMillis(30)
        val AbsensiCacheTTL: Long = TimeUnit.DAYS.toMillis(7)
    }
}

private class AndroidKeystoreJsonCipher {
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return "$PREFIX${iv.base64()}:${encrypted.base64()}"
    }

    fun decrypt(value: String): String {
        require(value.startsWith(PREFIX)) { "Unsupported encrypted cache format." }
        val payload = value.removePrefix(PREFIX)
        val parts = payload.split(":", limit = 2)
        require(parts.size == 2) { "Invalid encrypted cache payload." }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(GCM_TAG_BITS, Base64.decode(parts[0], Base64.NO_WRAP))
        )
        val decrypted = cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP))
        return decrypted.toString(Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return keyGenerator.generateKey()
    }

    private fun ByteArray.base64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "alhasanah_santri_detail_cache_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val PREFIX = "enc:v1:"
    }
}
