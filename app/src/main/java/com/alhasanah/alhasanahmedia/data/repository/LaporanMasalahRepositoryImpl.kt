package com.alhasanah.alhasanahmedia.data.repository

import android.content.Context
import android.os.Build
import com.alhasanah.alhasanahmedia.data.model.LaporanMasalah
import com.alhasanah.alhasanahmedia.data.model.LaporanMasalahInsert
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Locale
import java.util.TimeZone

class LaporanMasalahRepositoryImpl(
    private val postgrest: Postgrest
) : LaporanMasalahRepository {

    override suspend fun insertLaporan(laporan: LaporanMasalahInsert): LaporanMasalah? {
        return try {
            postgrest.from("laporan_masalah")
                .insert(laporan) {
                    select()
                }
                .decodeSingleOrNull<LaporanMasalah>()
        } catch (e: Exception) {
            null
        }
    }

    override fun getUserLaporans(userId: String): Flow<List<LaporanMasalah>> = flow {
        val result = postgrest.from("laporan_masalah")
            .select {
                filter {
                    eq("user_id", userId)
                }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeAs<List<LaporanMasalah>>()
        emit(result)
    }

    override suspend fun getLaporanById(id: String): LaporanMasalah? {
        return try {
            postgrest.from("laporan_masalah")
                .select {
                    filter {
                        eq("id", id)
                    }
                    limit(1)
                }
                .decodeSingleOrNull<LaporanMasalah>()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            appVersion = null,
            androidVersion = Build.VERSION.RELEASE,
            deviceBrand = Build.BRAND,
            deviceModel = Build.MODEL,
            deviceManufacturer = Build.MANUFACTURER,
            deviceSdk = Build.VERSION.SDK_INT,
            locale = Locale.getDefault().toString(),
            timezone = TimeZone.getDefault().id
        )
    }
}
