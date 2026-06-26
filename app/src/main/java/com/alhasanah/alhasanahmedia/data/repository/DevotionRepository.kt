package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.devotion.DevotionItem
import com.alhasanah.alhasanahmedia.data.model.devotion.DevotionLibraryData
import com.alhasanah.alhasanahmedia.data.model.devotion.KitabChapter
import kotlinx.coroutines.flow.Flow

interface DevotionRepository {
    fun getDevotions(): Flow<List<DevotionItem>>
    fun getLibrary(): Flow<Result<OfflineFirstResource<DevotionLibraryData>>>
    fun getKitabChapters(slug: String): Flow<Result<OfflineFirstResource<List<KitabChapter>>>>
    fun getKitabChapterDetail(slug: String, number: Int): Flow<Result<OfflineFirstResource<KitabChapter>>>
}
