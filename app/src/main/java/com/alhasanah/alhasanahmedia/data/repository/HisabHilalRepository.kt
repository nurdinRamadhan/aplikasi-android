package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.falak.FalakDataLengkap
import com.alhasanah.alhasanahmedia.data.model.falak.FalakEphemerisHarian
import com.alhasanah.alhasanahmedia.domain.falak.HasilHisabHilalEphemeris
import com.alhasanah.alhasanahmedia.domain.falak.HisabHilalEphemerisCalculator
import com.alhasanah.alhasanahmedia.domain.falak.KonteksHisabHilal
import java.time.LocalDate

data class DataEphemerisHisabHilal(
    val paketUtama: FalakDataLengkap,
    val paketPendukung: List<FalakDataLengkap>,
    val tanggalSituasiHilalMasehi: LocalDate,
    val ephemerisHarian: List<FalakEphemerisHarian>,
)

interface HisabHilalRepository {
    suspend fun persiapkanEphemeris(konteks: KonteksHisabHilal): Result<DataEphemerisHisabHilal>
    suspend fun hitung(konteks: KonteksHisabHilal): Result<HasilHisabHilalEphemeris>
}

class HisabHilalRepositoryImpl(
    private val falakRepository: FalakRepository,
    private val calculator: HisabHilalEphemerisCalculator,
) : HisabHilalRepository {

    override suspend fun persiapkanEphemeris(konteks: KonteksHisabHilal): Result<DataEphemerisHisabHilal> =
        runCatching {
            val tanggal = konteks.tanggalSituasiHilalMasehi
            val tanggalDiperlukan = setOf(tanggal, tanggal.plusDays(1))
            val paketPerTahun = tanggalDiperlukan
                .map { it.year }
                .distinct()
                .map { tahun ->
                    falakRepository.loadDataLengkap(tahun).recoverCatching {
                        falakRepository.refreshPaketKemenag(tahun).getOrThrow()
                    }.getOrThrow()
                }
            val ephemeris = paketPerTahun.flatMap { data ->
                data.ephemerisHarian.filter { item ->
                    runCatching { LocalDate.parse(item.date) }.getOrNull() in tanggalDiperlukan
                }
            }
            val tersedia = ephemeris.mapTo(mutableSetOf()) { LocalDate.parse(it.date) }
            val hilang = tanggalDiperlukan - tersedia
            check(hilang.isEmpty()) {
                "Data ephemeris tanggal ${hilang.joinToString()} belum tersedia pada paket ${paketPerTahun.joinToString { it.paket.kode }}."
            }
            DataEphemerisHisabHilal(
                paketUtama = paketPerTahun.first(),
                paketPendukung = paketPerTahun.drop(1),
                tanggalSituasiHilalMasehi = tanggal,
                ephemerisHarian = ephemeris.distinctBy { it.date }.sortedBy { it.date },
            )
        }

    override suspend fun hitung(konteks: KonteksHisabHilal): Result<HasilHisabHilalEphemeris> =
        runCatching {
            val siap = persiapkanEphemeris(konteks).getOrThrow()
            calculator.hitung(konteks, siap.ephemerisHarian)
        }
}
