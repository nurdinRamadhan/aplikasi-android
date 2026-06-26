package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.SantriModel
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class WaliSantriRepositoryImpl(
    private val postgrest: Postgrest,
    private val cacheStore: OfflineFirstCacheStore,
    private val authRepository: AuthRepository
) : WaliSantriRepository {

    override suspend fun getMySantriList(): List<SantriModel> {
        val userId = currentUserId()
        val cached = cacheStore.getSantriList(userId)
        return runCatching {
            postgrest.rpc("list_wali_santri_secure")
                .decodeList<SantriModel>()
                .also { cacheStore.saveSantriList(userId, it) }
        }.getOrElse { cached ?: throw it }
    }

    override suspend fun getSantriByNis(nis: String): SantriModel {
        val userId = currentUserId()
        val cached = cacheStore.getSantriDetail(userId, nis)
        return runCatching {
            postgrest.rpc(
                "get_wali_santri_detail_secure",
                WaliSantriDetailParams(nis = nis)
            ).decodeAs<SantriModel>()
                .also { cacheStore.saveSantriDetail(userId, it) }
        }.getOrElse { cached ?: throw it }
    }

    override suspend fun clearSensitiveCache() {
        cacheStore.clearSensitiveSantriDetails()
    }

    private suspend fun currentUserId(): String =
        authRepository.getCurrentUser().firstOrNull()?.id
            ?: error("Wali tidak ditemukan. Silakan login kembali.")
}

@Serializable
private data class WaliSantriDetailParams(
    @SerialName("p_nis")
    val nis: String,
    @SerialName("p_reason")
    val reason: String = "android_wali_detail"
)
