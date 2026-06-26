package com.alhasanah.alhasanahmedia.data.model.ibadah

import kotlinx.serialization.Serializable

@Serializable
data class IbadahGuideCatalog(
    val version: Int = 1,
    val updatedAt: String = "",
    val sources: List<IbadahSource> = emptyList(),
    val guides: List<IbadahGuide> = emptyList()
)

@Serializable
data class IbadahSource(
    val name: String,
    val url: String = ""
)

@Serializable
data class IbadahGuide(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val icon: String = "",
    val chapters: List<IbadahChapter> = emptyList()
)

@Serializable
data class IbadahChapter(
    val id: String,
    val title: String,
    val description: String = "",
    val steps: List<String> = emptyList(),
    val prayers: List<IbadahPrayer> = emptyList(),
    val notes: List<String> = emptyList()
)

@Serializable
data class IbadahPrayer(
    val title: String,
    val arabic: String = "",
    val latin: String = "",
    val translation: String = ""
)
