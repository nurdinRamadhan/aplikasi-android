package com.alhasanah.alhasanahmedia.data.model.qibla

import com.google.gson.annotations.SerializedName

data class QiblaResponse(
    @SerializedName("status")
    val status: Boolean,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("data")
    val data: QiblaData? = null
)

data class QiblaData(
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("direction")
    val direction: Double
)
