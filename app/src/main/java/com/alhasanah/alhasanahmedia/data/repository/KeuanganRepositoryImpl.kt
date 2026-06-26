package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.PembayaranTagihanDto
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

class KeuanganRepositoryImpl(
    private val supabaseClient: SupabaseClient,
    private val cacheStore: OfflineFirstCacheStore
) : KeuanganRepository {

    override fun getTagihanByNis(nis: String): Flow<List<TagihanWithDetail>> = flow {
        cacheStore.getTagihan(nis)?.let {
            emit(it)
        }

        supabaseClient.auth.awaitInitialization()
        supabaseClient.auth.currentUserOrNull()
            ?: error("Sesi login belum siap. Silakan muat ulang halaman tagihan.")

        val result = supabaseClient.from("tagihan_santri")
            .select(Columns.raw("*, ref_jenis_pembayaran(nama_pembayaran, tipe)")) {
                filter {
                    eq("santri_nis", nis)
                }
                order("tanggal_jatuh_tempo", Order.ASCENDING)
            }
            .decodeList<TagihanWithDetail>()

        cacheStore.saveTagihan(nis, result)
        emit(result)
    }.catch { error ->
        val cached = cacheStore.getTagihan(nis)
        if (!cached.isNullOrEmpty()) {
            emit(cached)
        } else {
            throw error
        }
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
