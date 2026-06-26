package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.islamiccalendar.CalendarData
import com.alhasanah.alhasanahmedia.data.model.islamiccalendar.HolidayItem
import com.alhasanah.alhasanahmedia.data.model.islamiccalendar.SunnahFastItem
import kotlinx.coroutines.flow.Flow

data class IslamicCalendarBundle(
    val calendar: CalendarData,
    val sunnahFasts: List<SunnahFastItem>,
    val holidays: List<HolidayItem>
)

interface IslamicCalendarRepository {
    fun getCalendarBundle(
        timezone: String = "Asia/Jakarta",
        method: String = "standar",
        adjustment: Int = 0
    ): Flow<Result<OfflineFirstResource<IslamicCalendarBundle>>>
}
