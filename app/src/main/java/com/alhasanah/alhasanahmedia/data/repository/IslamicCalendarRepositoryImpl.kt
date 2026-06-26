package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.islamiccalendar.CalendarData
import com.alhasanah.alhasanahmedia.data.model.islamiccalendar.HolidayItem
import com.alhasanah.alhasanahmedia.data.model.islamiccalendar.SunnahFastItem
import com.alhasanah.alhasanahmedia.data.remote.islamiccalendar.IslamicCalendarApiService
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class IslamicCalendarRepositoryImpl(
    private val apiService: IslamicCalendarApiService,
    private val cacheStore: OfflineFirstCacheStore
) : IslamicCalendarRepository {

    override fun getCalendarBundle(
        timezone: String,
        method: String,
        adjustment: Int
    ): Flow<Result<OfflineFirstResource<IslamicCalendarBundle>>> = flow {
        val today = today(timezone)
        val year = today.year
        val cachedCalendar = cacheStore.getIslamicCalendarToday(timezone, method, adjustment, today.toString())
        val cachedHolidays = cacheStore.getHolidayList(year)
        if (cachedCalendar != null) {
            emit(
                Result.success(
                    OfflineFirstResource(
                        data = cachedCalendar.value.toBundle(cachedHolidays?.value.orEmpty()),
                        isFromCache = true,
                        updatedAt = maxOf(cachedCalendar.updatedAt, cachedHolidays?.updatedAt ?: 0L)
                    )
                )
            )
        }

        try {
            val calendarResponse = apiService.getTodayCalendar(
                adjustment = adjustment,
                method = method,
                timezone = timezone
            )
            val calendar = calendarResponse.data
            if (!calendarResponse.status || calendar == null) {
                emitFailureOrCached(cachedCalendar?.value, cachedHolidays?.value.orEmpty(), calendarResponse.message ?: "Kalender tidak tersedia")
                return@flow
            }

            val holidays = runCatching { apiService.getHolidays(year = year) }
                .getOrNull()
                ?.takeIf { it.status }
                ?.data
                .orEmpty()

            cacheStore.saveIslamicCalendarToday(timezone, method, adjustment, today.toString(), calendar)
            if (holidays.isNotEmpty()) {
                cacheStore.saveHolidayList(year, holidays)
            }

            emit(
                Result.success(
                    OfflineFirstResource(
                        data = calendar.toBundle(if (holidays.isNotEmpty()) holidays else cachedHolidays?.value.orEmpty()),
                        isFromCache = false
                    )
                )
            )
        } catch (e: Exception) {
            emitFailureOrCached(
                calendar = cachedCalendar?.value,
                holidays = cachedHolidays?.value.orEmpty(),
                message = e.message ?: "Tidak dapat memperbarui kalender"
            )
        }
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<Result<OfflineFirstResource<IslamicCalendarBundle>>>.emitFailureOrCached(
        calendar: CalendarData?,
        holidays: List<HolidayItem>,
        message: String
    ) {
        if (calendar != null) {
            emit(
                Result.success(
                    OfflineFirstResource(
                        data = calendar.toBundle(holidays),
                        isFromCache = true,
                        notice = "Mode offline. Menampilkan kalender tersimpan."
                    )
                )
            )
        } else {
            emit(Result.failure(Exception(message)))
        }
    }

    private fun CalendarData.toBundle(holidays: List<HolidayItem>): IslamicCalendarBundle =
        IslamicCalendarBundle(
            calendar = this,
            sunnahFasts = sunnahFastItems(this),
            holidays = holidays
        )

    private fun sunnahFastItems(calendar: CalendarData): List<SunnahFastItem> {
        val dayName = calendar.ce.dayName.lowercase()
        val hijriDay = calendar.hijr.day
        return listOf(
            SunnahFastItem(
                title = "Puasa Senin Kamis",
                subtitle = if (dayName.contains("senin") || dayName.contains("kamis")) {
                    "Hari ini termasuk waktu yang dianjurkan."
                } else {
                    "Amalan pekanan setiap Senin dan Kamis."
                },
                activeToday = dayName.contains("senin") || dayName.contains("kamis")
            ),
            SunnahFastItem(
                title = "Puasa Ayyamul Bidh",
                subtitle = "Tanggal 13, 14, dan 15 Hijriah setiap bulan.",
                activeToday = hijriDay in 13..15
            )
        )
    }

    private fun today(timezone: String): LocalDate =
        runCatching { LocalDate.now(ZoneId.of(timezone)) }
            .getOrDefault(LocalDate.now(ZoneId.of("Asia/Jakarta")))
}
