package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.hadith.HadithExploreData
import com.alhasanah.alhasanahmedia.data.model.hadith.HadithItem
import com.alhasanah.alhasanahmedia.data.model.hadith.HadithSearchData
import com.alhasanah.alhasanahmedia.data.remote.hadith.HadithApiService
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap

class HadithRepositoryImpl(
    private val apiService: HadithApiService,
    private val cacheStore: OfflineFirstCacheStore
) : HadithRepository {

    private val detailCache = ConcurrentHashMap<Int, HadithItem>()

    override fun exploreHadith(page: Int, limit: Int): Flow<Result<OfflineFirstResource<HadithExploreData>>> = flow {
        val safeLimit = limit.coerceIn(1, 10)
        val cached = cacheStore.getHadithExplore(page, safeLimit)
        cached?.let {
            emit(Result.success(OfflineFirstResource(it.value, isFromCache = true, updatedAt = it.updatedAt)))
        }

        try {
            val response = apiService.exploreHadith(page = page, limit = safeLimit)
            val data = response.data
            if (response.status && data != null) {
                data.hadith.forEach { cacheStore.saveHadithDetail(it) }
                cacheStore.saveHadithExplore(page, safeLimit, data)
                emit(Result.success(OfflineFirstResource(data, isFromCache = false)))
            } else {
                emitFailureOrCached(cached, response.message ?: "Gagal mengambil daftar hadis")
            }
        } catch (e: Exception) {
            emitFailureOrCached(cached, e.message ?: "Tidak dapat memperbarui daftar hadis")
        }
    }

    override fun searchHadith(keyword: String, page: Int, limit: Int): Flow<Result<OfflineFirstResource<HadithSearchData>>> = flow {
        val trimmedKeyword = keyword.trim()
        if (trimmedKeyword.length < 4) {
            emit(Result.failure(Exception("Kata kunci minimal 4 karakter")))
            return@flow
        }

        val safeLimit = limit.coerceIn(1, 20)
        val cached = cacheStore.getHadithSearch(trimmedKeyword, page, safeLimit)
        cached?.let {
            emit(Result.success(OfflineFirstResource(it.value, isFromCache = true, updatedAt = it.updatedAt)))
        }

        try {
            val response = apiService.searchHadith(
                keyword = trimmedKeyword,
                page = page,
                limit = safeLimit
            )
            val data = response.data
            if (response.status && data != null) {
                cacheStore.saveHadithSearch(trimmedKeyword, page, safeLimit, data)
                emit(Result.success(OfflineFirstResource(data, isFromCache = false)))
            } else {
                emitFailureOrCached(cached, response.message ?: "Gagal mencari hadis")
            }
        } catch (e: Exception) {
            emitFailureOrCached(cached, e.message ?: "Tidak dapat memperbarui pencarian hadis")
        }
    }

    override fun getHadithDetail(id: Int): Flow<Result<OfflineFirstResource<HadithItem>>> = flow {
        val memoryCached = detailCache[id]
        val cached = cacheStore.getHadithDetail(id)
        when {
            memoryCached != null -> emit(Result.success(OfflineFirstResource(memoryCached, isFromCache = true)))
            cached != null -> {
                detailCache[id] = cached.value
                emit(Result.success(OfflineFirstResource(cached.value, isFromCache = true, updatedAt = cached.updatedAt)))
            }
        }

        try {
            val response = apiService.getHadithDetail(id)
            val data = response.data
            if (response.status && data != null) {
                detailCache[id] = data
                cacheStore.saveHadithDetail(data)
                emit(Result.success(OfflineFirstResource(data, isFromCache = false)))
            } else {
                emitFailureOrCached(cached ?: memoryCached?.let { CachedValue(it, System.currentTimeMillis()) }, response.message ?: "Hadis tidak ditemukan")
            }
        } catch (e: Exception) {
            emitFailureOrCached(cached ?: memoryCached?.let { CachedValue(it, System.currentTimeMillis()) }, e.message ?: "Tidak dapat memperbarui hadis")
        }
    }

    override fun getNextHadith(id: Int): Flow<Result<OfflineFirstResource<HadithItem>>> = flow {
        val adjacentCache = detailCache[id]?.next?.let { nextId ->
            cacheStore.getHadithDetail(nextId)
        } ?: cacheStore.getHadithDetail(id)?.value?.next?.let { nextId ->
            cacheStore.getHadithDetail(nextId)
        }
        try {
            val response = apiService.getNextHadith(id)
            val data = response.data
            if (response.status && data != null) {
                detailCache[data.id] = data
                cacheStore.saveHadithDetail(data)
                emit(Result.success(OfflineFirstResource(data, isFromCache = false)))
            } else {
                emitFailureOrCached(adjacentCache, response.message ?: "Hadis berikutnya tidak tersedia")
            }
        } catch (e: Exception) {
            emitFailureOrCached(adjacentCache, e.message ?: "Hadis berikutnya tidak tersedia saat offline")
        }
    }

    override fun getPreviousHadith(id: Int): Flow<Result<OfflineFirstResource<HadithItem>>> = flow {
        val adjacentCache = detailCache[id]?.prev?.let { previousId ->
            cacheStore.getHadithDetail(previousId)
        } ?: cacheStore.getHadithDetail(id)?.value?.prev?.let { previousId ->
            cacheStore.getHadithDetail(previousId)
        }
        try {
            val response = apiService.getPreviousHadith(id)
            val data = response.data
            if (response.status && data != null) {
                detailCache[data.id] = data
                cacheStore.saveHadithDetail(data)
                emit(Result.success(OfflineFirstResource(data, isFromCache = false)))
            } else {
                emitFailureOrCached(adjacentCache, response.message ?: "Hadis sebelumnya tidak tersedia")
            }
        } catch (e: Exception) {
            emitFailureOrCached(adjacentCache, e.message ?: "Hadis sebelumnya tidak tersedia saat offline")
        }
    }

    private suspend fun <T> FlowCollector<Result<OfflineFirstResource<T>>>.emitFailureOrCached(
        cached: CachedValue<T>?,
        message: String
    ) {
        if (cached != null) {
            emit(
                Result.success(
                    OfflineFirstResource(
                        data = cached.value,
                        isFromCache = true,
                        updatedAt = cached.updatedAt,
                        notice = "Mode offline. Menampilkan data tersimpan."
                    )
                )
            )
        } else {
            emit(Result.failure(Exception(message)))
        }
    }
}
