package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.Berita
import com.alhasanah.alhasanahmedia.data.model.PublicPrestasiCategoryCount
import com.alhasanah.alhasanahmedia.data.model.PublicPrestasiSantri
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class PublicRepository(private val postgrest: Postgrest) {

    /**
     * Fetches a list of published news articles, ordered by publishing date.
     */
    suspend fun getBeritaList(): List<Berita> {
        return postgrest.from("berita").select {
            filter {
                eq("status", "PUBLISHED")
            }
            order("tanggal_publish", Order.DESCENDING)
        }.decodeAs()
    }

    /**
     * Fetches a single news article by its slug. Returns null if not found.
     */
    suspend fun getBeritaBySlug(slug: String): Berita? {
        return postgrest.from("berita")
            .select {
                filter {
                    eq("slug", slug)
                    eq("status", "PUBLISHED")
                }
                limit(1)
            }
            .decodeAs<List<Berita>>()
            .firstOrNull()
    }

    suspend fun getPrestasiList(
        kategori: String? = null,
        search: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<PublicPrestasiSantri> {
        return postgrest.rpc(
            "get_public_prestasi_santri",
            PublicPrestasiParams(
                kategori = kategori,
                search = search,
                limit = limit,
                offset = offset
            )
        ).decodeList()
    }

    suspend fun getPrestasiCategoryCounts(search: String? = null): List<PublicPrestasiCategoryCount> {
        return postgrest.rpc(
            "get_public_prestasi_category_counts",
            PublicPrestasiCountParams(search = search)
        ).decodeList()
    }


    /**
     * Fetches public information about the institution.
     * This is an example of a query that does not require authentication.
     */
    suspend fun getInstansiInfo() {
        // Example: return postgrest.from("instansi_info").select().single().decodeAs<InstansiInfoModel>()
        // For now, we'll leave it as a placeholder.
    }
}

@Serializable
private data class PublicPrestasiParams(
    @SerialName("p_kategori")
    val kategori: String? = null,
    @SerialName("p_search")
    val search: String? = null,
    @SerialName("p_limit")
    val limit: Int = 50,
    @SerialName("p_offset")
    val offset: Int = 0
)

@Serializable
private data class PublicPrestasiCountParams(
    @SerialName("p_search")
    val search: String? = null
)
