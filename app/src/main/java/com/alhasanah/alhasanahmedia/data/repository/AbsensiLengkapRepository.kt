package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.AbsensiLengkapResponse
import kotlinx.coroutines.flow.Flow

interface AbsensiLengkapRepository {
    fun getAbsensiLengkap(
        nis: String,
        startDate: String,
        endDate: String
    ): Flow<AbsensiLengkapResponse>
}