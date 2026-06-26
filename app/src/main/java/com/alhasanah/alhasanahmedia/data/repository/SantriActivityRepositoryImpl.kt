package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.HafalanTahfidz
import com.alhasanah.alhasanahmedia.data.model.HafalanKitab
import com.alhasanah.alhasanahmedia.data.model.KesehatanSantri
import com.alhasanah.alhasanahmedia.data.model.MurojaahTahfidz
import com.alhasanah.alhasanahmedia.data.model.PelanggaranSantri
import com.alhasanah.alhasanahmedia.data.model.PerizinanSantri
import com.alhasanah.alhasanahmedia.data.model.RingkasanAbsensiMingguan
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

class SantriActivityRepositoryImpl(
    private val postgrest: Postgrest,
    private val cacheStore: OfflineFirstCacheStore
) : SantriActivityRepository {
    override fun getHafalan(nis: String): Flow<List<HafalanTahfidz>> = flow {
        cacheStore.getHafalanTahfidz(nis)?.let { emit(it) }
        val result = postgrest.from("hafalan_tahfidz").select {
            filter {
                eq("santri_nis", nis)
            }
            order("tanggal", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            order("id", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
        }.decodeAs<List<HafalanTahfidz>>()
        cacheStore.saveHafalanTahfidz(nis, result)
        emit(result)
    }

    override fun getMurojaah(nis: String): Flow<List<MurojaahTahfidz>> = flow {
        cacheStore.getMurojaahTahfidz(nis)?.let { emit(it) }
        val result = postgrest.from("murojaah_tahfidz").select {
            filter {
                eq("santri_nis", nis)
            }
            order("tanggal", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            order("id", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
        }.decodeAs<List<MurojaahTahfidz>>()
        cacheStore.saveMurojaahTahfidz(nis, result)
        emit(result)
    }

    override fun getPelanggaran(nis: String): Flow<List<PelanggaranSantri>> = flow {
        cacheStore.getPelanggaran(nis)?.let { emit(it) }
        val result = postgrest.from("pelanggaran_santri").select {
            filter {
                eq("santri_nis", nis)
            }
            order("tanggal", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            order("id", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
        }.decodeAs<List<PelanggaranSantri>>()
        cacheStore.savePelanggaran(nis, result)
        emit(result)
    }

    override fun getPerizinan(nis: String): Flow<List<PerizinanSantri>> = flow {
        cacheStore.getPerizinan(nis)?.let { emit(it) }
        val result = postgrest.from("perizinan_santri").select {
            filter {
                eq("santri_nis", nis)
            }
            order("tanggal", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            order("id", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
        }.decodeAs<List<PerizinanSantri>>()
        cacheStore.savePerizinan(nis, result)
        emit(result)
    }

    override fun getKesehatan(nis: String): Flow<List<KesehatanSantri>> = flow {
        cacheStore.getKesehatan(nis)?.let { emit(it) }
        val result = postgrest.from("kesehatan_santri").select {
            filter {
                eq("santri_nis", nis)
            }
            order("tanggal", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            order("id", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
        }.decodeAs<List<KesehatanSantri>>()
        cacheStore.saveKesehatan(nis, result)
        emit(result)
    }

    override fun getHafalanKitab(nis: String): Flow<List<HafalanKitab>> = flow {
        cacheStore.getHafalanKitab(nis)?.let { emit(it) }
        val result = postgrest.from("hafalan_kitab").select {
            filter {
                eq("santri_nis", nis)
            }
            order("tanggal", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            order("id", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
        }.decodeAs<List<HafalanKitab>>()
        cacheStore.saveHafalanKitab(nis, result)
        emit(result)
    }

    @Serializable
    private data class RingkasanAbsensiParams(
        @SerialName("p_santri_nis") val santriNis: String,
        @SerialName("p_start_date") val startDate: String
    )

    override fun getRingkasanAbsensiMingguan(nis: String, weekStart: String): Flow<RingkasanAbsensiMingguan> = flow {
        cacheStore.getRingkasanAbsensiMingguan(nis, weekStart)?.let { emit(it) }
        val result = postgrest.rpc("get_ringkasan_absensi_mingguan", RingkasanAbsensiParams(nis, weekStart))
            .decodeAs<RingkasanAbsensiMingguan>()
        cacheStore.saveRingkasanAbsensiMingguan(nis, weekStart, result)
        emit(result)
    }
}
