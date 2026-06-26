package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.HafalanTahfidz
import com.alhasanah.alhasanahmedia.data.model.HafalanKitab
import com.alhasanah.alhasanahmedia.data.model.KesehatanSantri
import com.alhasanah.alhasanahmedia.data.model.MurojaahTahfidz
import com.alhasanah.alhasanahmedia.data.model.PelanggaranSantri
import com.alhasanah.alhasanahmedia.data.model.PerizinanSantri
import com.alhasanah.alhasanahmedia.data.model.RingkasanAbsensiMingguan
import kotlinx.coroutines.flow.Flow

interface SantriActivityRepository {
    fun getHafalan(nis: String): Flow<List<HafalanTahfidz>>
    fun getMurojaah(nis: String): Flow<List<MurojaahTahfidz>>
    fun getPelanggaran(nis: String): Flow<List<PelanggaranSantri>>
    fun getPerizinan(nis: String): Flow<List<PerizinanSantri>>
    fun getKesehatan(nis: String): Flow<List<KesehatanSantri>>
    fun getHafalanKitab(nis: String): Flow<List<HafalanKitab>>
    fun getRingkasanAbsensiMingguan(nis: String, weekStart: String): Flow<RingkasanAbsensiMingguan>
}
