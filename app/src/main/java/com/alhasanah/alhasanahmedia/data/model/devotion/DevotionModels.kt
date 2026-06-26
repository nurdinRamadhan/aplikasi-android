package com.alhasanah.alhasanahmedia.data.model.devotion

import kotlinx.serialization.Serializable

@Serializable
data class DevotionItem(
    val id: String,
    val title: String,
    val category: String,
    val arabic: String,
    val latin: String,
    val translation: String,
    val source: String = "",
    val fawaid: String = "",
    val note: String = ""
)

@Serializable
data class DevotionCategory(
    val name: String,
    val slug: String,
    val total: Int = 0
)

@Serializable
data class KitabBook(
    val slug: String,
    val title: String,
    val author: String = "",
    val category: String = "",
    val description: String = "",
    val totalChapters: Int = 0
)

@Serializable
data class KitabChapter(
    val id: String,
    val bookSlug: String,
    val number: Int,
    val title: String,
    val arabic: String = "",
    val latin: String = "",
    val translation: String = "",
    val content: String = ""
)

@Serializable
data class DevotionLibraryData(
    val categories: List<DevotionCategory> = emptyList(),
    val devotions: List<DevotionItem> = emptyList(),
    val kitabCategories: List<String> = emptyList(),
    val kitabBooks: List<KitabBook> = emptyList()
)
