package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.falak.FalakCacheStatus
import com.alhasanah.alhasanahmedia.data.model.falak.FalakDataLengkap
import com.alhasanah.alhasanahmedia.data.model.falak.FalakEphemerisHarian
import com.alhasanah.alhasanahmedia.data.model.falak.FalakManifest
import com.alhasanah.alhasanahmedia.data.model.falak.FalakManifestJumlah
import com.alhasanah.alhasanahmedia.data.model.falak.FalakPaketDataDto
import com.alhasanah.alhasanahmedia.data.model.falak.FalakRentangTanggal
import com.alhasanah.alhasanahmedia.domain.falak.HisabHilalEphemerisCalculator
import com.alhasanah.alhasanahmedia.domain.falak.KonteksHisabHilal
import com.alhasanah.alhasanahmedia.domain.falak.MarkazFalak
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class HisabHilalRepositoryTest {

    @Test
    fun persiapkanEphemerisMengambilTanggalSituasiDanTanggalBerikutnyaLintasTahun() = runBlocking {
        val fake = FakeFalakRepository(
            mapOf(
                2026 to paket(2026, listOf("2026-12-31")),
                2027 to paket(2027, listOf("2027-01-01")),
            )
        )
        val repository = HisabHilalRepositoryImpl(fake, HisabHilalEphemerisCalculator())
        val konteks = KonteksHisabHilal(
            bulanHijriah = "Rajab 1448 H",
            tanggalSituasiHilalMasehi = LocalDate.parse("2026-12-31"),
            markaz = MarkazFalak("Markaz Uji", -6.2, 106.8, 100.0),
        )

        val result = repository.persiapkanEphemeris(konteks).getOrThrow()

        assertEquals(listOf("2026-12-31", "2027-01-01"), result.ephemerisHarian.map { it.date })
        assertEquals(2026, result.paketUtama.paket.tahun)
        assertEquals(listOf(2027), result.paketPendukung.map { it.paket.tahun })
        assertEquals(listOf(2026, 2027), fake.loadedYears)
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
