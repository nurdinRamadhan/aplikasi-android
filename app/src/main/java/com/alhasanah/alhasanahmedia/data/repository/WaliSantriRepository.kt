package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.SantriModel

interface WaliSantriRepository {
    suspend fun getMySantriList(): List<SantriModel>
    suspend fun getSantriByNis(nis: String): SantriModel
    suspend fun clearSensitiveCache()
}
