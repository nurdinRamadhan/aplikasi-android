package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.AbsensiLengkapResponse
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class AbsensiLengkapParams(
    @SerialName("p_nis") val nis: String,
    @SerialName("p_start_date") val startDate: String,
    @SerialName("p_end_date") val endDate: String
)

class AbsensiLengkapRepositoryImpl(
    private val postgrest: Postgrest
) : AbsensiLengkapRepository {

    override fun getAbsensiLengkap(
        nis: String,
        startDate: String,
        endDate: String
    ): Flow<AbsensiLengkapResponse> = flow {
        val result = postgrest.rpc(
            "get_absensi_lengkap",
            AbsensiLengkapParams(
                nis = nis,
                startDate = startDate,
                endDate = endDate
            )
        ).decodeAs<AbsensiLengkapResponse>()
        emit(result)
    }
}