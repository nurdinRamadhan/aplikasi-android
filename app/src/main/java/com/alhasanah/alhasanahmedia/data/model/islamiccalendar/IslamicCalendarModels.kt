package com.alhasanah.alhasanahmedia.data.model.islamiccalendar

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CalendarDateInfo(
    @SerialName("today")
    @SerializedName("today")
    val today: String = "",
    @SerialName("day")
    @SerializedName("day")
    val day: Int = 0,
    @SerialName("dayName")
    @SerializedName("dayName")
    val dayName: String = "",
    @SerialName("month")
    @SerializedName("month")
    val month: Int = 0,
    @SerialName("monthName")
    @SerializedName("monthName")
    val monthName: String = "",
    @SerialName("year")
    @SerializedName("year")
    val year: Int = 0
)

@Serializable
data class CalendarData(
    @SerialName("method")
    @SerializedName("method")
    val method: String = "standar",
    @SerialName("adjustment")
    @SerializedName("adjustment")
    val adjustment: Int = 0,
    @SerialName("ce")
    @SerializedName("ce")
    val ce: CalendarDateInfo = CalendarDateInfo(),
    @SerialName("hijr")
    @SerializedName("hijr")
    val hijr: CalendarDateInfo = CalendarDateInfo()
)

@Serializable
data class CalendarResponse(
    @SerialName("status")
    @SerializedName("status")
    val status: Boolean,
    @SerialName("message")
    @SerializedName("message")
    val message: String? = null,
    @SerialName("data")
    @SerializedName("data")
    val data: CalendarData? = null
)

@Serializable
data class HolidayItem(
    @SerialName("date")
    @SerializedName("date")
    val date: String? = null,
    @SerialName("tanggal")
    @SerializedName("tanggal")
    val tanggal: String? = null,
    @SerialName("name")
    @SerializedName("name")
    val name: String? = null,
    @SerialName("nama")
    @SerializedName("nama")
    val nama: String? = null,
    @SerialName("description")
    @SerializedName("description")
    val description: String? = null,
    @SerialName("keterangan")
    @SerializedName("keterangan")
    val keterangan: String? = null,
    @SerialName("is_national_holiday")
    @SerializedName("is_national_holiday")
    val isNationalHoliday: Boolean? = null
) {
    val displayDate: String
        get() = date ?: tanggal ?: ""

    val displayName: String
        get() = name ?: nama ?: description ?: keterangan ?: "Hari libur"
}

@Serializable
data class HolidayResponse(
    @SerialName("status")
    @SerializedName("status")
    val status: Boolean,
    @SerialName("message")
    @SerializedName("message")
    val message: String? = null,
    @SerialName("data")
    @SerializedName("data")
    val data: List<HolidayItem> = emptyList()
)

@Serializable
data class SunnahFastItem(
    val title: String,
    val subtitle: String,
    val activeToday: Boolean
)
