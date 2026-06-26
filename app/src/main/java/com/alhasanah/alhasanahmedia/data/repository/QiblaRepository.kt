package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.qibla.QiblaData
import kotlinx.coroutines.flow.Flow

interface QiblaRepository {
    fun getQiblaDirection(latitude: Double, longitude: Double): Flow<Result<QiblaData>>
}
