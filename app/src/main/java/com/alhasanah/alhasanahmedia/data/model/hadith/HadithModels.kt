package com.alhasanah.alhasanahmedia.data.model.hadith

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HadithText(
    @SerialName("ar")
    @SerializedName("ar")
    val arabic: String? = null,
    @SerialName("id")
    @SerializedName("id")
    val indonesia: String? = null
)

@Serializable
data class HadithItem(
    @SerialName("id")
    @SerializedName("id")
    val id: Int,
    @SerialName("text")
    @SerializedName("text")
    val text: HadithText? = null,
    @SerialName("grade")
    @SerializedName("grade")
    val grade: String? = null,
    @SerialName("takhrij")
    @SerializedName("takhrij")
    val takhrij: String? = null,
    @SerialName("hikmah")
    @SerializedName("hikmah")
    val hikmah: String? = null,
    @SerialName("prev")
    @SerializedName("prev")
    val prev: Int? = null,
    @SerialName("next")
    @SerializedName("next")
    val next: Int? = null
)

@Serializable
data class HadithPaging(
    @SerialName("current")
    @SerializedName("current")
    val current: Int = 1,
    @SerialName("per_page")
    @SerializedName("per_page")
    val perPage: Int = 10,
    @SerialName("total_data")
    @SerializedName("total_data")
    val totalData: Int = 0,
    @SerialName("total_pages")
    @SerializedName("total_pages")
    val totalPages: Int = 1,
    @SerialName("has_prev")
    @SerializedName("has_prev")
    val hasPrev: Boolean = false,
    @SerialName("has_next")
    @SerializedName("has_next")
    val hasNext: Boolean = false,
    @SerialName("next_page")
    @SerializedName("next_page")
    val nextPage: Int? = null,
    @SerialName("prev_page")
    @SerializedName("prev_page")
    val prevPage: Int? = null,
    @SerialName("first_page")
    @SerializedName("first_page")
    val firstPage: Int? = null,
    @SerialName("last_page")
    @SerializedName("last_page")
    val lastPage: Int? = null
)

@Serializable
data class HadithExploreData(
    @SerialName("paging")
    @SerializedName("paging")
    val paging: HadithPaging = HadithPaging(),
    @SerialName("hadis")
    @SerializedName("hadis")
    val hadith: List<HadithItem> = emptyList()
)

@Serializable
data class HadithExploreResponse(
    @SerialName("status")
    @SerializedName("status")
    val status: Boolean,
    @SerialName("message")
    @SerializedName("message")
    val message: String? = null,
    @SerialName("data")
    @SerializedName("data")
    val data: HadithExploreData? = null
)

@Serializable
data class HadithDetailResponse(
    @SerialName("status")
    @SerializedName("status")
    val status: Boolean,
    @SerialName("message")
    @SerializedName("message")
    val message: String? = null,
    @SerialName("data")
    @SerializedName("data")
    val data: HadithItem? = null
)

@Serializable
data class HadithSearchItem(
    @SerialName("id")
    @SerializedName("id")
    val id: Int,
    @SerialName("text")
    @SerializedName("text")
    val text: String? = null,
    @SerialName("focus")
    @SerializedName("focus")
    val focus: List<String> = emptyList()
)

@Serializable
data class HadithSearchData(
    @SerialName("keyword")
    @SerializedName("keyword")
    val keyword: String = "",
    @SerialName("paging")
    @SerializedName("paging")
    val paging: HadithPaging = HadithPaging(),
    @SerialName("hadis")
    @SerializedName("hadis")
    val hadith: List<HadithSearchItem> = emptyList()
)

@Serializable
data class HadithSearchResponse(
    @SerialName("status")
    @SerializedName("status")
    val status: Boolean,
    @SerialName("message")
    @SerializedName("message")
    val message: String? = null,
    @SerialName("data")
    @SerializedName("data")
    val data: HadithSearchData? = null
)
