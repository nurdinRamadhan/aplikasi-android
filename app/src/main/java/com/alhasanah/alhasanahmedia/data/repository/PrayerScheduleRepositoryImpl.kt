package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.prayer.PrayerLocation
import com.alhasanah.alhasanahmedia.data.model.prayer.PrayerScheduleData
import com.alhasanah.alhasanahmedia.data.remote.prayer.PrayerScheduleApiService
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException

class PrayerScheduleRepositoryImpl(
    private val apiService: PrayerScheduleApiService,
    private val cacheStore: OfflineFirstCacheStore
) : PrayerScheduleRepository {

    override fun searchLocations(keyword: String): Flow<Result<OfflineFirstResource<List<PrayerLocation>>>> = flow {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) {
            emit(Result.failure(Exception("Masukkan nama kota atau kabupaten")))
            return@flow
        }

        val cached = cacheStore.getPrayerLocations(trimmed)
        cached?.let {
            emit(Result.success(OfflineFirstResource(it.value, isFromCache = true, updatedAt = it.updatedAt)))
        }

        try {
            val response = apiService.searchLocations(trimmed)
            if (response.status) {
                cacheStore.savePrayerLocations(trimmed, response.data)
                emit(Result.success(OfflineFirstResource(response.data, isFromCache = false)))
            } else {
                emitFailureOrCached(
                    cached = cached,
                    message = response.message ?: "Lokasi tidak ditemukan"
                )
            }
        } catch (e: Exception) {
            emitFailureOrCached(
                cached = cached,
                message = e.toPrayerMessage("Lokasi tidak ditemukan")
            )
        }
    }

    override fun getTodaySchedule(locationId: String, timezone: String): Flow<Result<OfflineFirstResource<PrayerScheduleData>>> = flow {
        val dateKey = todayKey(timezone)
        val cached = cacheStore.getPrayerTodaySchedule(locationId, timezone, dateKey)
        cached?.let {
            emit(Result.success(OfflineFirstResource(it.value, isFromCache = true, updatedAt = it.updatedAt)))
        }

        try {
            val response = runCatching {
                apiService.getTodaySchedule(locationId, timezone)
            }.getOrElse { error ->
                if (error is HttpException && error.code() == 404) {
                    apiService.getScheduleByPeriod(locationId, dateKey)
                } else {
                    throw error
                }
            }
            val data = response.data
            if (response.status && data != null) {
                cacheStore.savePrayerTodaySchedule(locationId, timezone, dateKey, data)
                emit(Result.success(OfflineFirstResource(data, isFromCache = false)))
            } else {
                emitFailureOrCached(
                    cached = cached,
                    message = response.message ?: "Jadwal sholat tidak ditemukan"
                )
            }
        } catch (e: Exception) {
            emitFailureOrCached(
                cached = cached,
                message = e.toPrayerMessage("Jadwal sholat tidak ditemukan untuk lokasi ini")
            )
        }
    }

    override fun getMonthlySchedule(locationId: String, yearMonth: String): Flow<Result<OfflineFirstResource<PrayerScheduleData>>> = flow {
        val cached = cacheStore.getPrayerMonthlySchedule(locationId, yearMonth)
        cached?.let {
            emit(Result.success(OfflineFirstResource(it.value, isFromCache = true, updatedAt = it.updatedAt)))
        }

        try {
            val response = apiService.getScheduleByPeriod(locationId, yearMonth)
            val data = response.data
            if (response.status && data != null) {
                cacheStore.savePrayerMonthlySchedule(locationId, yearMonth, data)
                emit(Result.success(OfflineFirstResource(data, isFromCache = false)))
            } else {
                emitFailureOrCached(
                    cached = cached,
                    message = response.message ?: "Jadwal sholat bulanan tidak ditemukan"
                )
            }
        } catch (e: Exception) {
            emitFailureOrCached(
                cached = cached,
                message = e.toPrayerMessage("Jadwal sholat bulanan tidak ditemukan untuk lokasi ini")
            )
        }
    }

    private suspend fun <T> FlowCollector<Result<OfflineFirstResource<T>>>.emitFailureOrCached(
        cached: CachedValue<T>?,
        message: String
    ) {
        if (cached != null) {
            emit(
                Result.success(
                    OfflineFirstResource(
                        data = cached.value,
                        isFromCache = true,
                        updatedAt = cached.updatedAt,
                        notice = "Mode offline. Menampilkan data tersimpan."
                    )
                )
            )
        } else {
            emit(Result.failure(Exception(message)))
        }
    }

    private fun todayKey(timezone: String): String =
        runCatching { LocalDate.now(ZoneId.of(timezone)).toString() }
            .getOrDefault(LocalDate.now(ZoneId.of("Asia/Jakarta")).toString())

    private fun Exception.toPrayerMessage(fallback: String): String =
        if (this is HttpException) {
            when (code()) {
                400 -> "Format permintaan jadwal sholat tidak valid"
                404 -> fallback
                else -> "Server jadwal sholat sedang tidak tersedia (${code()})"
            }
        } else {
            message ?: fallback
        }
}
