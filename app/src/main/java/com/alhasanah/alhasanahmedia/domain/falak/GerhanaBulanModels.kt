package com.alhasanah.alhasanahmedia.domain.falak

import java.time.LocalDate

enum class ModeDataGerhanaBulan {
    AcuanKemenag,
    InputManual,
}

data class KonteksGerhanaBulan(
    val bulanHijriah: String,
    val tanggalKemungkinanGerhanaMasehi: LocalDate,
    val zonaWaktu: ZonaWaktuFalak = ZonaWaktuFalak.WIB,
    val koreksiIstiqbalJam: Double = (1.0 / 60.0) + (49.29 / 3600.0),
    val modeData: ModeDataGerhanaBulan = ModeDataGerhanaBulan.AcuanKemenag,
    val rentangPencarianManualHari: Int = 3,
)

data class PembandingMeeusGerhanaBulan(
    val tanggalPurnamaTerdekatUt: LocalDate,
    val jamPurnamaTerdekatUt: Double,
    val selisihHariDariInput: Double,
    val argumenLintangBulanDerajat: Double,
    val jarakKeSimpulDerajat: Double,
    val memungkinkanGerhana: Boolean,
    val catatan: String,
)

data class DataIstiqbalGerhanaBulan(
    val fibTerbesarPersen: NilaiEphemerisFalak,
    val jamFibUt: Int,
    val elmJamFib: NilaiEphemerisFalak,
    val elmJamSetelahnya: NilaiEphemerisFalak,
    val albJamFib: NilaiEphemerisFalak,
    val albJamSetelahnya: NilaiEphemerisFalak,
    val lintangBulanJamFib: NilaiEphemerisFalak,
)

data class SabaqGerhanaBulan(
    val sabaqMatahariDerajat: Double,
    val sabaqBulanDerajat: Double,
    val sabaqBulanMatahariDerajat: Double,
)

data class SaatIstiqbalGerhanaBulan(
    val mbDerajat: Double,
    val titikIstiqbalJam: Double,
    val koreksiIstiqbalJam: Double,
    val waktuUt: WaktuFalak,
    val waktuLokal: WaktuFalak,
)

data class DataIstiqbalTerinterpolasiGerhanaBulan(
    val semiDiameterBulan: InterpolasiEphemerisFalak,
    val horizontalParallaxBulan: InterpolasiEphemerisFalak,
    val lintangBulan: InterpolasiEphemerisFalak,
    val semiDiameterMatahari: InterpolasiEphemerisFalak,
    val jarakBumiMatahari: InterpolasiAngkaEphemerisFalak,
)

data class StatusKemungkinanGerhanaBulan(
    val nilaiMutlakLintangDerajat: Double,
    val status: String,
    val memungkinkanGerhana: Boolean,
)

data class BayanganBumiGerhanaBulan(
    val parallaxMatahariDerajat: Double,
    val parallaxBulanDerajat: Double,
    val semiDiameterBayanganIntiDerajat: Double,
    val xDerajat: Double,
    val yDerajat: Double,
)

data class SimpulGerhanaBulan(
    val hDerajat: Double,
    val uDerajat: Double,
    val zDerajat: Double,
    val kDerajatPerJam: Double,
)

enum class JenisGerhanaBulan {
    TidakTerjadi,
    PenumbraSebagian,
    PenumbraTotal,
    Sebagian,
    Total,
}

data class KlasifikasiGerhanaBulan(
    val jenis: JenisGerhanaBulan,
    val keterangan: String,
    val memenuhiKontakUmbra: Boolean,
    val memenuhiKontakTotal: Boolean,
)

data class JarakKontakGerhanaBulan(
    val cDerajat: Double,
    val eDerajat: Double?,
    val t1Jam: Double,
    val t2Jam: Double?,
)

data class KoreksiTengahGerhanaBulan(
    val ta: Double,
    val tb: Double,
    val t0Jam: Double,
    val deltaTJam: Double,
)

data class WaktuKontakGerhanaBulan(
    val tengahGerhanaUt: WaktuFalak,
    val tengahGerhanaLokal: WaktuFalak,
    val mulaiGerhanaLokal: WaktuFalak?,
    val mulaiTotalLokal: WaktuFalak?,
    val selesaiTotalLokal: WaktuFalak?,
    val selesaiGerhanaLokal: WaktuFalak?,
)

data class MagnitudeGerhanaBulan(
    val magnitudeUmbra: Double?,
)

data class KesimpulanGerhanaBulan(
    val jenis: JenisGerhanaBulan,
    val status: String,
    val tengahGerhanaLokal: WaktuFalak?,
    val mulaiGerhanaLokal: WaktuFalak?,
    val mulaiTotalLokal: WaktuFalak?,
    val selesaiTotalLokal: WaktuFalak?,
    val selesaiGerhanaLokal: WaktuFalak?,
)

data class HasilGerhanaBulanEphemeris(
    val konteks: KonteksGerhanaBulan,
    val pembandingMeeus: PembandingMeeusGerhanaBulan?,
    val dataIstiqbal: DataIstiqbalGerhanaBulan,
    val sabaq: SabaqGerhanaBulan,
    val saatIstiqbal: SaatIstiqbalGerhanaBulan,
    val statusKemungkinan: StatusKemungkinanGerhanaBulan,
    val dataInterpolasi: DataIstiqbalTerinterpolasiGerhanaBulan,
    val bayanganBumi: BayanganBumiGerhanaBulan,
    val simpul: SimpulGerhanaBulan,
    val klasifikasi: KlasifikasiGerhanaBulan,
    val jarakKontak: JarakKontakGerhanaBulan,
    val koreksiTengah: KoreksiTengahGerhanaBulan,
    val waktuKontak: WaktuKontakGerhanaBulan,
    val magnitude: MagnitudeGerhanaBulan,
    val kesimpulan: KesimpulanGerhanaBulan,
    val butirPerhitungan: List<ButirPerhitunganFalak>,
)
