package com.alhasanah.alhasanahmedia.data.remote.qibla

import com.alhasanah.alhasanahmedia.data.model.qibla.QiblaResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface QiblaApiService {
    @GET("qibla/{coordinate}")
    suspend fun getQiblaDirection(
        @Path(value = "coordinate", encoded = true) coordinate: String
    ): QiblaResponse
}
