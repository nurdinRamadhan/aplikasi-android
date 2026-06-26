package com.alhasanah.alhasanahmedia.data.model.prayer

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PrayerLocation(
    @SerialName("id")
    @SerializedName("id")
    val id: String,
    @SerialName("lokasi")
    @SerializedName("lokasi")
    val lokasi: String
)

@Serializable
data class PrayerLocationResponse(
    @SerialName("status")
    @SerializedName("status")
    val status: Boolean,
    @SerialName("message")
    @SerializedName("message")
    val message: String? = null,
    @SerialName("data")
    @SerializedName("data")
    val data: List<PrayerLocation> = emptyList()
)

@Serializable
data class PrayerScheduleEntry(
    @SerialName("tanggal")
    @SerializedName("tanggal")
    val tanggal: String = "",
    @SerialName("imsak")
    @SerializedName("imsak")
    val imsak: String = "",
    @SerialName("subuh")
    @SerializedName("subuh")
    val subuh: String = "",
    @SerialName("terbit")
    @SerializedName("terbit")
    val terbit: String = "",
    @SerialName("dhuha")
    @SerializedName("dhuha")
    val dhuha: String = "",
    @SerialName("dzuhur")
    @SerializedName("dzuhur")
    val dzuhur: String = "",
    @SerialName("ashar")
    @SerializedName("ashar")
    val ashar: String = "",
    @SerialName("maghrib")
    @SerializedName("maghrib")
    val maghrib: String = "",
    @SerialName("isya")
    @SerializedName("isya")
    val isya: String = ""
)

@Serializable
data class PrayerScheduleData(
    @SerialName("id")
    @SerializedName("id")
    val id: String = "",
    @SerialName("kabko")
    @SerializedName("kabko")
    val kabko: String = "",
    @SerialName("prov")
    @SerializedName("prov")
    val prov: String = "",
    @SerialName("jadwal")
    @SerializedName("jadwal")
    val jadwal: Map<String, PrayerScheduleEntry> = emptyMap()
)

@Serializable
data class PrayerScheduleResponse(
    @SerialName("status")
    @SerializedName("status")
    val status: Boolean,
    @SerialName("message")
    @SerializedName("message")
    val message: String? = null,
    @SerialName("data")
    @SerializedName("data")
    val data: PrayerScheduleData? = null
)
