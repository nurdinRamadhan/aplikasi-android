package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.hadith.HadithExploreData
import com.alhasanah.alhasanahmedia.data.model.hadith.HadithItem
import com.alhasanah.alhasanahmedia.data.model.hadith.HadithSearchData
import kotlinx.coroutines.flow.Flow

interface HadithRepository {
    fun exploreHadith(page: Int = 1, limit: Int = 10): Flow<Result<OfflineFirstResource<HadithExploreData>>>
    fun searchHadith(keyword: String, page: Int = 1, limit: Int = 10): Flow<Result<OfflineFirstResource<HadithSearchData>>>
    fun getHadithDetail(id: Int): Flow<Result<OfflineFirstResource<HadithItem>>>
    fun getNextHadith(id: Int): Flow<Result<OfflineFirstResource<HadithItem>>>
    fun getPreviousHadith(id: Int): Flow<Result<OfflineFirstResource<HadithItem>>>
}
