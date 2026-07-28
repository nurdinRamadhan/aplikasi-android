package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.LaporanMasalah
import com.alhasanah.alhasanahmedia.data.model.LaporanMasalahInsert
import kotlinx.coroutines.flow.Flow

interface LaporanMasalahRepository {
    suspend fun insertLaporan(laporan: LaporanMasalahInsert): LaporanMasalah?
    fun getUserLaporans(userId: String): Flow<List<LaporanMasalah>>
    suspend fun getLaporanById(id: String): LaporanMasalah?
    suspend fun getDeviceInfo(): DeviceInfo
}

data class DeviceInfo(
    val appVersion: String?,
    val androidVersion: String?,
    val deviceBrand: String?,
    val deviceModel: String?,
    val deviceManufacturer: String?,
    val deviceSdk: Int?,
    val locale: String?,
    val timezone: String?
)
