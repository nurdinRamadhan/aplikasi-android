package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.falak.FalakDataLengkap
import com.alhasanah.alhasanahmedia.data.model.falak.FalakEphemerisHarian
import com.alhasanah.alhasanahmedia.domain.falak.GerhanaBulanEphemerisCalculator
import com.alhasanah.alhasanahmedia.domain.falak.HasilGerhanaBulanEphemeris
import com.alhasanah.alhasanahmedia.domain.falak.KonteksGerhanaBulan
import com.alhasanah.alhasanahmedia.domain.falak.ModeDataGerhanaBulan
import java.time.LocalDate

data class DataEphemerisGerhanaBulan(
    val paketUtama: FalakDataLengkap,
    val paketPendukung: List<FalakDataLengkap>,
    val tanggalKemungkinanGerhanaMasehi: LocalDate,
    val ephemerisHarian: List<FalakEphemerisHarian>,
)

interface GerhanaBulanRepository {
    suspend fun persiapkanEphemeris(konteks: KonteksGerhanaBulan): Result<DataEphemerisGerhanaBulan>
    suspend fun hitung(konteks: KonteksGerhanaBulan): Result<HasilGerhanaBulanEphemeris>
}

class GerhanaBulanRepositoryImpl(
    private val falakRepository: FalakRepository,
    private val calculator: GerhanaBulanEphemerisCalculator,
) : GerhanaBulanRepository {

    override suspend fun persiapkanEphemeris(konteks: KonteksGerhanaBulan): Result<DataEphemerisGerhanaBulan> =
        runCatching {
            val tanggal = konteks.tanggalKemungkinanGerhanaMasehi
            val tanggalAcuan = setOf(tanggal.minusDays(1), tanggal, tanggal.plusDays(1))
            val tanggalDiperlukan = if (konteks.modeData == ModeDataGerhanaBulan.InputManual) {
                val rentang = konteks.rentangPencarianManualHari.coerceAtLeast(1)
                (-rentang.toLong()..rentang.toLong() + 1L).mapTo(mutableSetOf()) { tanggal.plusDays(it) }
            } else {
                (-2L..2L).mapTo(mutableSetOf()) { tanggal.plusDays(it) }
            }
            val tahunDiperlukan = tanggalDiperlukan
                .map { it.year }
                .distinct()
            val wajibLengkap = konteks.modeData == ModeDataGerhanaBulan.AcuanKemenag
            var paketPerTahun = loadPaketPerTahun(tahunDiperlukan, wajibLengkap)
            var ephemeris = filterEphemeris(paketPerTahun, tanggalDiperlukan)
            var tersedia = ephemeris.mapTo(mutableSetOf()) { LocalDate.parse(it.date) }
            var hilang = tanggalDiperlukan - tersedia
            if (hilang.isNotEmpty()) {
                paketPerTahun = if (wajibLengkap) {
                    tahunDiperlukan.map { tahun ->
                        falakRepository.refreshPaketKemenag(tahun).getOrThrow()
                    }
                } else {
                    val refresh = tahunDiperlukan.mapNotNull { tahun ->
                        falakRepository.refreshPaketKemenag(tahun).getOrNull()
                    }
                    (refresh + paketPerTahun).distinctBy { it.paket.tahun }
                }
                ephemeris = filterEphemeris(paketPerTahun, tanggalDiperlukan)
                tersedia = ephemeris.mapTo(mutableSetOf()) { LocalDate.parse(it.date) }
                hilang = tanggalDiperlukan - tersedia
            }
            if (konteks.modeData == ModeDataGerhanaBulan.AcuanKemenag) {
                val hilangAcuan = tanggalAcuan - tersedia
                check(hilangAcuan.isEmpty()) {
                    "Data ephemeris ${hilangAcuan.joinToString()} belum tersedia. Mode acuan Kemenag membutuhkan data H-1, H, dan H+1 dari tanggal acuan ${tanggal}. Sinkronkan paket Kemenag untuk tahun ${tahunDiperlukan.joinToString()}."
                }
            } else {
                check(ephemeris.isNotEmpty()) {
                    "Data ephemeris sekitar tanggal ${tanggal} belum tersedia. Mode input manual membutuhkan minimal satu tanggal ephemeris di rentang pencarian manual."
                }
            }
            DataEphemerisGerhanaBulan(
                paketUtama = paketPerTahun.first(),
                paketPendukung = paketPerTahun.drop(1),
                tanggalKemungkinanGerhanaMasehi = tanggal,
                ephemerisHarian = ephemeris.distinctBy { it.date }.sortedBy { it.date },
            )
        }

    override suspend fun hitung(konteks: KonteksGerhanaBulan): Result<HasilGerhanaBulanEphemeris> =
        runCatching {
            val siap = persiapkanEphemeris(konteks).getOrThrow()
            calculator.hitung(konteks, siap.ephemerisHarian)
        }

    private suspend fun loadPaketPerTahun(
        tahunDiperlukan: List<Int>,
        wajibLengkap: Boolean,
    ): List<FalakDataLengkap> =
        tahunDiperlukan.mapNotNull { tahun ->
            val paket = falakRepository.loadDataLengkap(tahun).recoverCatching {
                falakRepository.refreshPaketKemenag(tahun).getOrThrow()
            }
            if (wajibLengkap) paket.getOrThrow() else paket.getOrNull()
        }

    private fun filterEphemeris(
        paketPerTahun: List<FalakDataLengkap>,
        tanggalDiperlukan: Set<LocalDate>,
    ): List<FalakEphemerisHarian> =
        paketPerTahun.flatMap { data ->
            data.ephemerisHarian.filter { item ->
                runCatching { LocalDate.parse(item.date) }.getOrNull() in tanggalDiperlukan
            }
        }
}
