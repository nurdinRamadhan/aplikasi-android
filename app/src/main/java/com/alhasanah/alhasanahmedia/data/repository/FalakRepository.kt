package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.falak.FalakCacheStatus
import com.alhasanah.alhasanahmedia.data.model.falak.FalakDataLengkap
import com.alhasanah.alhasanahmedia.data.model.falak.FalakEphemerisHarian
import com.alhasanah.alhasanahmedia.data.model.falak.FalakHalamanPdf
import com.alhasanah.alhasanahmedia.data.model.falak.FalakHilalTable
import com.alhasanah.alhasanahmedia.data.model.falak.FalakIndeksItem
import kotlinx.coroutines.flow.Flow

interface FalakRepository {
    fun observeCacheStatus(): Flow<FalakCacheStatus>
    suspend fun refreshPaketKemenag(tahun: Int = 2026): Result<FalakDataLengkap>
    suspend fun loadDataLengkap(): Result<FalakDataLengkap>
    suspend fun loadDataLengkap(tahun: Int): Result<FalakDataLengkap>
    suspend fun cariIndeks(query: String, tipe: String? = null, limit: Int = 200): List<FalakIndeksItem>
    suspend fun getEphemerisTanggal(tanggal: String): FalakEphemerisHarian?
    suspend fun getHilalTable(index: Int): FalakHilalTable?
    suspend fun getHalamanPdf(nomorHalaman: Int): FalakHalamanPdf?
}
