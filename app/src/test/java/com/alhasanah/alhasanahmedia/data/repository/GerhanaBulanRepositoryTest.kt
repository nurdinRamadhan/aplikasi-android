package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.falak.FalakCacheStatus
import com.alhasanah.alhasanahmedia.data.model.falak.FalakDataLengkap
import com.alhasanah.alhasanahmedia.data.model.falak.FalakEphemerisHarian
import com.alhasanah.alhasanahmedia.data.model.falak.FalakManifest
import com.alhasanah.alhasanahmedia.data.model.falak.FalakManifestJumlah
import com.alhasanah.alhasanahmedia.data.model.falak.FalakPaketDataDto
import com.alhasanah.alhasanahmedia.data.model.falak.FalakRentangTanggal
import com.alhasanah.alhasanahmedia.domain.falak.GerhanaBulanEphemerisCalculator
import com.alhasanah.alhasanahmedia.domain.falak.KonteksGerhanaBulan
import com.alhasanah.alhasanahmedia.domain.falak.ModeDataGerhanaBulan
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GerhanaBulanRepositoryTest {

    @Test
    fun persiapkanEphemerisMengambilTanggalSebelumSaatDanSesudahLintasTahun() = runBlocking {
        val fake = FakeFalakRepository(
            mapOf(
                2026 to paket(2026, listOf("2026-12-31")),
                2027 to paket(2027, listOf("2027-01-01", "2027-01-02")),
            )
        )
        val repository = GerhanaBulanRepositoryImpl(fake, GerhanaBulanEphemerisCalculator())
        val konteks = KonteksGerhanaBulan(
            bulanHijriah = "Pertengahan Rajab 1448 H",
            tanggalKemungkinanGerhanaMasehi = LocalDate.parse("2027-01-01"),
        )

        val result = repository.persiapkanEphemeris(konteks).getOrThrow()

        assertEquals(listOf("2026-12-31", "2027-01-01", "2027-01-02"), result.ephemerisHarian.map { it.date })
        assertEquals(2026, result.paketUtama.paket.tahun)
        assertEquals(listOf(2027), result.paketPendukung.map { it.paket.tahun })
        assertEquals(listOf(2026, 2027), fake.loadedYears)
    }

    @Test
    fun inputManualTidakMemaksaSemuaTanggalAcuanKemenagTersedia() = runBlocking {
        val fake = FakeFalakRepository(
            mapOf(
                2026 to paket(2026, listOf("2026-03-04")),
            )
        )
        val repository = GerhanaBulanRepositoryImpl(fake, GerhanaBulanEphemerisCalculator())
        val konteks = KonteksGerhanaBulan(
            bulanHijriah = "Uji Manual",
            tanggalKemungkinanGerhanaMasehi = LocalDate.parse("2026-03-03"),
            modeData = ModeDataGerhanaBulan.InputManual,
        )

        val result = repository.persiapkanEphemeris(konteks).getOrThrow()

        assertEquals(listOf("2026-03-04"), result.ephemerisHarian.map { it.date })
        assertEquals(listOf(2026), fake.loadedYears)
    }

    @Test
    fun inputManualMenyiapkanBufferInterpolasiSampaiDuaHariSetelahTanggalInput() = runBlocking {
        val fake = FakeFalakRepository(
            mapOf(
                2026 to paket(
                    2026,
                    listOf(
                        "2026-08-16",
                        "2026-08-17",
                        "2026-08-18",
                        "2026-08-19",
                        "2026-08-20",
                        "2026-08-21",
                        "2026-08-22",
                        "2026-08-23",
                    )
                ),
            )
        )
        val repository = GerhanaBulanRepositoryImpl(fake, GerhanaBulanEphemerisCalculator())
        val konteks = KonteksGerhanaBulan(
            bulanHijriah = "Uji Manual",
            tanggalKemungkinanGerhanaMasehi = LocalDate.parse("2026-08-19"),
            modeData = ModeDataGerhanaBulan.InputManual,
        )

        val result = repository.persiapkanEphemeris(konteks).getOrThrow()

        assertEquals(
            listOf(
                "2026-08-16",
                "2026-08-17",
                "2026-08-18",
                "2026-08-19",
                "2026-08-20",
                "2026-08-21",
                "2026-08-22",
                "2026-08-23",
            ),
            result.ephemerisHarian.map { it.date }
        )
    }

    private fun paket(tahun: Int, tanggal: List<String>): FalakDataLengkap {
        val kode = "kemenag-$tahun"
        return FalakDataLengkap(
            paket = FalakPaketDataDto(
                id = kode,
                kode = kode,
                judul = "Kemenag $tahun",
                tahun = tahun,
                versi = "uji",
                jenisSumber = "kemenag",
                sumberResmi = "Kemenag",
                status = "aktif",
            ),
            manifest = FalakManifest(
                schemaVersion = 1,
                kode = kode,
                judul = "Kemenag $tahun",
                tahun = tahun,
                versi = "uji",
                jenisSumber = "kemenag",
                sumberResmi = "Kemenag",
                bucket = "falak-ephemeris",
                storagePrefix = "kemenag/$tahun",
                zonaWaktuData = "UT",
                rentangTanggal = FalakRentangTanggal(
                    mulai = tanggal.minOrNull(),
                    selesai = tanggal.maxOrNull(),
                ),
                jumlah = FalakManifestJumlah(hariEphemeris = tanggal.size),
                berkas = emptyList(),
            ),
            ephemerisHarian = tanggal.map { FalakEphemerisHarian(date = it) },
            hilalLokasi = emptyList(),
            indeks = emptyList(),
        )
    }

    private class FakeFalakRepository(
        private val paketPerTahun: Map<Int, FalakDataLengkap>,
    ) : FalakRepository {
        val loadedYears = mutableListOf<Int>()

        override fun observeCacheStatus(): Flow<FalakCacheStatus> = emptyFlow()

        override suspend fun refreshPaketKemenag(tahun: Int): Result<FalakDataLengkap> =
            paketPerTahun[tahun]?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("Tidak ada paket $tahun"))

        override suspend fun loadDataLengkap(): Result<FalakDataLengkap> =
            Result.success(paketPerTahun.values.first())

        override suspend fun loadDataLengkap(tahun: Int): Result<FalakDataLengkap> {
            loadedYears += tahun
            return paketPerTahun[tahun]?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("Tidak ada paket $tahun"))
        }

        override suspend fun cariIndeks(query: String, tipe: String?, limit: Int) = emptyList<com.alhasanah.alhasanahmedia.data.model.falak.FalakIndeksItem>()

        override suspend fun getEphemerisTanggal(tanggal: String): FalakEphemerisHarian? = null

        override suspend fun getHilalTable(index: Int) = null

        override suspend fun getHalamanPdf(nomorHalaman: Int) = null
    }
}
