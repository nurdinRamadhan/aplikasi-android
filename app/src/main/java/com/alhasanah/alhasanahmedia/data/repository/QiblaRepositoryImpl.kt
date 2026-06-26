package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.qibla.QiblaData
import com.alhasanah.alhasanahmedia.data.remote.qibla.QiblaApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Locale

class QiblaRepositoryImpl(
    private val apiService: QiblaApiService
) : QiblaRepository {

    override fun getQiblaDirection(latitude: Double, longitude: Double): Flow<Result<QiblaData>> = flow {
        try {
            val coordinate = String.format(Locale.US, "%.6f,%.6f", latitude, longitude)
            val response = apiService.getQiblaDirection(coordinate)
            val data = response.data
            if (response.status && data != null) {
                emit(Result.success(data))
            } else {
                emit(Result.failure(Exception(response.message ?: "Gagal mengambil arah kiblat")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
