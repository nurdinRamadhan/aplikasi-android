package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.prayer.PrayerLocation
import com.alhasanah.alhasanahmedia.data.model.prayer.PrayerScheduleData
import kotlinx.coroutines.flow.Flow

interface PrayerScheduleRepository {
    fun searchLocations(keyword: String): Flow<Result<OfflineFirstResource<List<PrayerLocation>>>>
    fun getTodaySchedule(locationId: String, timezone: String = "Asia/Jakarta"): Flow<Result<OfflineFirstResource<PrayerScheduleData>>>
    fun getMonthlySchedule(locationId: String, yearMonth: String): Flow<Result<OfflineFirstResource<PrayerScheduleData>>>
}
