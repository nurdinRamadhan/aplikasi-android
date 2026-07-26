package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.PembayaranTagihanDto
import com.alhasanah.alhasanahmedia.data.model.TagihanCache
import com.alhasanah.alhasanahmedia.data.model.TagihanWithDetail
import com.alhasanah.alhasanahmedia.data.model.TransaksiDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.OffsetDateTime
import java.time.ZoneOffset

class KeuanganRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val cacheStore: OfflineFirstCacheStore
) : KeuanganRepository {

    // Selalu refetch dari server setiap kali halaman dibuka (financial data harus fresh)
    private val STALE_THRESHOLD_MS = 0L

    override fun getTagihanByNis(nis: String): Flow<TagihanCache> = flow {
        // 1. Emit cache dulu (instant UI)
        val cache = cacheStore.getTagihan(nis)
        cache?.let {
            emit(it)
        }

        // 2. Cek apakah perlu fetch ulang
        val shouldFetch = cache == null || isStale(cache)

        if (!shouldFetch) {
            // Cache fresh — skip network
            return@flow
        }

        // 3. Background fetch dengan conditional (ETag via updated_at)
        supabaseClient.auth.awaitInitialization()
        supabaseClient.auth.currentUserOrNull()
            ?: error("Sesi login belum siap. Silakan muat ulang halaman tagihan.")

        val lastSync = cache?.serverSyncedAt
        val selectBuilder = supabaseClient.from("tagihan_santri")
            .select(Columns.raw("*, ref_jenis_pembayaran(nama_pembayaran, tipe)")) {
                filter {
                    eq("santri_nis", nis)
                    if (lastSync != null && lastSync > 0) {
                        val lastSyncDateTime = OffsetDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(lastSync), java.time.ZoneOffset.UTC
                        )
                        gt("updated_at", lastSyncDateTime.toString())
                    }
                }
                order("tanggal_jatuh_tempo", Order.ASCENDING)
            }

        val result = selectBuilder.decodeList<TagihanWithDetail>()

        // 4. Merge dengan cache existing
        val merged = mergeWithCache(cache?.items ?: emptyList(), result)

        // 5. Save cache dengan metadata baru
        val newEtag = generateEtag(merged)
        val newCache = TagihanCache(
            items = merged,
            serverSyncedAt = System.currentTimeMillis(),
            etag = newEtag
        )
        cacheStore.saveTagihan(nis, newCache)

        emit(newCache)
    }.catch { error ->
        val cached = cacheStore.getTagihan(nis)
        if (cached != null && cached.items.isNotEmpty()) {
            emit(cached)
        } else {
            throw error
        }
    }

    private fun isStale(cache: TagihanCache): Boolean {
        val syncedAt = cache.serverSyncedAt
        return syncedAt == null || (System.currentTimeMillis() - syncedAt) > STALE_THRESHOLD_MS
    }

    private fun mergeWithCache(
        cached: List<TagihanWithDetail>,
        fresh: List<TagihanWithDetail>
    ): List<TagihanWithDetail> {
        val cachedMap = cached.associateBy { it.id }
        val freshMap = fresh.associateBy { it.id }
        return (cachedMap.keys + freshMap.keys)
            .distinct()
            .mapNotNull { id ->
                freshMap[id] ?: cachedMap[id]
            }
    }

    private fun generateEtag(items: List<TagihanWithDetail>): String {
        val content = items.joinToString("|") { "${it.id}:${it.updatedAt ?: it.createdAt}" }
        return content.hashCode().toString()
    }

    override suspend fun getPembayaranTagihan(tagihanId: String): List<PembayaranTagihanDto> {
        supabaseClient.auth.awaitInitialization()
        supabaseClient.auth.currentUserOrNull()
            ?: error("Sesi login belum siap. Silakan muat ulang halaman tagihan.")

        return supabaseClient.from("pembayaran_tagihan")
            .select {
                filter {
                    eq("tagihan_id", tagihanId)
                }
                order("paid_at", Order.DESCENDING)
                order("created_at", Order.DESCENDING)
            }
            .decodeList<PembayaranTagihanDto>()
    }

    override suspend fun getPembayaranTagihanByNis(nis: String): List<PembayaranTagihanDto> {
        supabaseClient.auth.awaitInitialization()
        supabaseClient.auth.currentUserOrNull()
            ?: error("Sesi login belum siap. Silakan muat ulang halaman tagihan.")

        return supabaseClient.from("pembayaran_tagihan")
            .select {
                filter {
                    eq("santri_nis", nis)
                }
                order("paid_at", Order.DESCENDING)
                order("created_at", Order.DESCENDING)
            }
            .decodeList<PembayaranTagihanDto>()
    }

    override suspend fun createTransaksiKeuangan(transaksi: TransaksiDto) {
        supabaseClient.from("transaksi_keuangan").insert(transaksi)
    }
}
