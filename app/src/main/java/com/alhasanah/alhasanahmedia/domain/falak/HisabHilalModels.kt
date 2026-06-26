package com.alhasanah.alhasanahmedia.domain.falak

import java.time.LocalDate

data class ZonaWaktuFalak(
    val nama: String,
    val offsetJam: Double,
    val bujurStandarDerajat: Double,
) {
    companion object {
        val WIB = ZonaWaktuFalak("WIB", 7.0, 105.0)
        val WITA = ZonaWaktuFalak("WITA", 8.0, 120.0)
        val WIT = ZonaWaktuFalak("WIT", 9.0, 135.0)
    }
}

data class MarkazFalak(
    val nama: String,
    val lintangDerajat: Double,
    val bujurDerajat: Double,
    val elevasiMeter: Double,
    val zonaWaktu: ZonaWaktuFalak = ZonaWaktuFalak.WIB,
)

data class KonteksHisabHilal(
    val bulanHijriah: String,
    val tanggalSituasiHilalMasehi: LocalDate,
    val markaz: MarkazFalak,
    val jamGhurubPerkiraanLokal: Double = 18.0,
    val kriteriaAwalBulan: KriteriaAwalBulanFalak = KriteriaAwalBulanFalak.KemenagMabimsTerbaru,
)

data class KriteriaAwalBulanFalak(
    val nama: String,
    val tinggiHilalMinimumDerajat: Double?,
    val elongasiMinimumDerajat: Double?,
    val memakaiSyaratIjtimaSebelumGhurub: Boolean = true,
) {
    companion object {
        val KemenagMabimsTerbaru = KriteriaAwalBulanFalak(
            nama = "Kemenag/MABIMS terbaru",
            tinggiHilalMinimumDerajat = 3.0,
            elongasiMinimumDerajat = 6.4,
        )

        val HisabWujudulHilal = KriteriaAwalBulanFalak(
            nama = "Hisab wujudul hilal",
            tinggiHilalMinimumDerajat = 0.0,
            elongasiMinimumDerajat = null,
        )

        val TanpaKriteria = KriteriaAwalBulanFalak(
            nama = "Tanpa kriteria visibilitas",
            tinggiHilalMinimumDerajat = null,
            elongasiMinimumDerajat = null,
            memakaiSyaratIjtimaSebelumGhurub = false,
        )
    }
}

data class WaktuFalak(
    val tanggal: LocalDate,
    val jamDesimal: Double,
    val zona: String,
)

data class SumberEphemerisFalak(
    val tanggal: LocalDate,
    val jamUt: Int,
    val namaTabel: String,
    val namaKolom: String,
    val raw: String?,
)

data class NilaiEphemerisFalak(
    val nilai: Double,
    val raw: String?,
    val sumber: SumberEphemerisFalak,
)

data class ButirPerhitunganFalak(
    val nomor: Int,
    val judul: String,
    val rumus: String,
    val substitusi: String,
    val hasil: String,
    val catatan: String? = null,
    val sumber: List<SumberEphemerisFalak> = emptyList(),
)

data class DataIjtima(
    val fibTerkecilPersen: NilaiEphemerisFalak,
    val jamFibUt: Int,
    val albJamFib: NilaiEphemerisFalak,
    val albJamSetelahnya: NilaiEphemerisFalak,
    val elmJamFib: NilaiEphemerisFalak,
    val elmJamSetelahnya: NilaiEphemerisFalak,
)

data class SabaqIjtima(
    val sabaqBulanDerajat: Double,
    val sabaqMatahariDerajat: Double,
)

data class SaatIjtima(
    val jarakElmAlbDerajat: Double,
    val waktuUt: WaktuFalak,
    val waktuLokal: WaktuFalak,
)

data class PosisiMatahariHaqiqiGhurub(
    val jamAcuanUt: Int,
    val semiDiameterMatahariDerajat: NilaiEphemerisFalak,
    val refraksiGhurubDerajat: Double,
    val dipDerajat: Double,
    val tinggiMatahariHaqiqiDerajat: Double,
)

data class SudutWaktuMatahariGhurub(
    val jamAcuanUt: Int,
    val deklinasiMatahariDerajat: NilaiEphemerisFalak,
    val tinggiMatahariHaqiqiDerajat: Double,
    val argumenCosinus: Double,
    val sudutWaktuDerajat: Double,
)

data class KoreksiWaktuDaerah(
    val bujurStandarDerajat: Double,
    val bujurMarkazDerajat: Double,
    val koreksiJam: Double,
)

data class SaatGhurub(
    val sudutWaktuMatahariDerajat: Double,
    val equationOfTimeJam: NilaiEphemerisFalak,
    val koreksiWaktuDaerahJam: Double,
    val waktuLokal: WaktuFalak,
)

data class InterpolasiEphemerisFalak(
    val jamAtasUt: Int,
    val jamBawahUt: Int,
    val nc: Double,
    val nilaiAtas: NilaiEphemerisFalak,
    val nilaiBawah: NilaiEphemerisFalak,
    val hasilDerajat: Double,
)

data class InterpolasiAngkaEphemerisFalak(
    val jamAtasUt: Int,
    val jamBawahUt: Int,
    val nc: Double,
    val nilaiAtas: NilaiEphemerisFalak,
    val nilaiBawah: NilaiEphemerisFalak,
    val hasil: Double,
    val satuan: String,
)

data class AsensiorektaMatahariGhurub(
    val interpolasi: InterpolasiEphemerisFalak,
)

data class AsensiorektaBulanGhurub(
    val interpolasi: InterpolasiEphemerisFalak,
)

data class SudutWaktuBulanGhurub(
    val asensiorektaMatahariDerajat: Double,
    val asensiorektaBulanDerajat: Double,
    val sudutWaktuMatahariDerajat: Double,
    val sudutWaktuBulanDerajat: Double,
)

data class DeklinasiGhurub(
    val matahari: InterpolasiEphemerisFalak,
    val bulan: InterpolasiEphemerisFalak,
)

data class TinggiBulanHaqiqiGhurub(
    val lintangMarkazDerajat: Double,
    val deklinasiBulanDerajat: Double,
    val sudutWaktuBulanDerajat: Double,
    val argumenSinus: Double,
    val tinggiBulanHaqiqiDerajat: Double,
)

data class ParallaxBulanGhurub(
    val horizontalParallax: InterpolasiEphemerisFalak,
    val tinggiBulanHaqiqiDerajat: Double,
    val parallaxDerajat: Double,
)

data class SemiDiameterBulanGhurub(
    val interpolasi: InterpolasiEphemerisFalak,
)

data class HoBulanGhurub(
    val tinggiBulanHaqiqiDerajat: Double,
    val parallaxDerajat: Double,
    val semiDiameterBulanDerajat: Double,
    val hoDerajat: Double,
)

data class RefraksiHilal(
    val hoDerajat: Double,
    val refraksiDerajat: Double,
    val menggunakanRefraksiRataRata: Boolean,
)

data class TinggiBulanMariGhurub(
    val hoDerajat: Double,
    val refraksiDerajat: Double,
    val dipDerajat: Double,
    val tinggiBulanMariDerajat: Double,
)

data class NishfulFadhlahBulan(
    val lintangMarkazDerajat: Double,
    val deklinasiBulanDerajat: Double,
    val argumenSinus: Double,
    val nfDerajat: Double,
)

data class ParallaxNishfulFadhlah(
    val nfDerajat: Double,
    val horizontalParallaxDerajat: Double,
    val pnfDerajat: Double,
)

data class SetengahBusurSiangBulanHaqiqi(
    val nfDerajat: Double,
    val sbshDerajat: Double,
)

data class SetengahBusurSiangBulan(
    val sbshDerajat: Double,
    val nfDerajat: Double,
    val pnfDerajat: Double,
    val semiDiameterBulanDerajat: Double,
    val refraksiRataRataDerajat: Double,
    val dipDerajat: Double,
    val menggunakanRumusSbshLebihDari90: Boolean,
    val sbsDerajat: Double,
)

data class LamaHilalMukuts(
    val sbsDerajat: Double,
    val sudutWaktuBulanDerajat: Double,
    val lamaHilalJam: Double,
)

data class TerbenamHilal(
    val ghurub: WaktuFalak,
    val lamaHilalJam: Double,
    val waktuLokal: WaktuFalak,
)

data class AzimutMatahariGhurub(
    val lintangMarkazDerajat: Double,
    val sudutWaktuMatahariDerajat: Double,
    val deklinasiMatahariDerajat: Double,
    val argumenTangen: Double,
    val azimutDerajat: Double,
    val arahDariBarat: String,
)

data class AzimutBulanGhurub(
    val lintangMarkazDerajat: Double,
    val sudutWaktuBulanDerajat: Double,
    val deklinasiBulanDerajat: Double,
    val argumenTangen: Double,
    val azimutDerajat: Double,
    val arahDariBarat: String,
)

data class PosisiHilal(
    val azimutBulanDerajat: Double,
    val azimutMatahariDerajat: Double,
    val posisiHilalDerajat: Double,
    val arahDariMatahari: String,
)

data class ArahTerbenamHilal(
    val lintangMarkazDerajat: Double,
    val sbsDerajat: Double,
    val deklinasiBulanDerajat: Double,
    val argumenTangen: Double,
    val arahTerbenamDerajat: Double,
)

data class LuasCahayaHilal(
    val fibGhurub: InterpolasiAngkaEphemerisFalak,
)

data class LebarNurulHilal(
    val posisiHilalDerajat: Double,
    val tinggiBulanMariDerajat: Double,
    val nurulHilalJari: Double,
)

data class KemiringanHilal(
    val posisiHilalDerajat: Double,
    val tinggiBulanMariDerajat: Double,
    val kemiringanDerajat: Double,
    val keadaan: String,
)

data class JarakBusurElongasi(
    val deklinasiMatahariDerajat: Double,
    val deklinasiBulanDerajat: Double,
    val asensiorektaMatahariDerajat: Double,
    val asensiorektaBulanDerajat: Double,
    val argumenCosinus: Double,
    val elongasiDerajat: Double,
)

data class KesimpulanHisabHilal(
    val bulanHijriah: String,
    val kriteriaAwalBulan: KriteriaAwalBulanFalak,
    val tanggalSituasiHilalMasehi: LocalDate,
    val tanggalPrakiraanAwalBulanMasehi: LocalDate,
    val ijtima: WaktuFalak,
    val ghurub: WaktuFalak,
    val tinggiHilalMariDerajat: Double,
    val lamaHilalJam: Double,
    val terbenamHilal: WaktuFalak,
    val azimutMatahariDerajat: Double,
    val azimutHilalDerajat: Double,
    val posisiHilalDerajat: Double,
    val posisiHilalDariMatahari: String,
    val keadaanHilal: String,
    val fractionIlluminationPersen: Double,
    val cahayaHilalJari: Double,
    val elongasiHilalDerajat: Double,
    val arahTerbenamHilalDerajat: Double,
    val ijtimaSebelumGhurub: Boolean,
    val tinggiHilalMemenuhi: Boolean?,
    val elongasiMemenuhi: Boolean?,
    val memenuhiKriteria: Boolean,
    val statusPrakiraan: String,
    val catatan: String,
)

data class HasilHisabHilalEphemeris(
    val konteks: KonteksHisabHilal,
    val dataIjtima: DataIjtima,
    val sabaqIjtima: SabaqIjtima,
    val saatIjtima: SaatIjtima,
    val posisiMatahariHaqiqiGhurub: PosisiMatahariHaqiqiGhurub,
    val sudutWaktuMatahariGhurub: SudutWaktuMatahariGhurub,
    val koreksiWaktuDaerah: KoreksiWaktuDaerah,
    val saatGhurub: SaatGhurub,
    val asensiorektaMatahariGhurub: AsensiorektaMatahariGhurub,
    val asensiorektaBulanGhurub: AsensiorektaBulanGhurub,
    val sudutWaktuBulanGhurub: SudutWaktuBulanGhurub,
    val deklinasiGhurub: DeklinasiGhurub,
    val tinggiBulanHaqiqiGhurub: TinggiBulanHaqiqiGhurub,
    val parallaxBulanGhurub: ParallaxBulanGhurub,
    val semiDiameterBulanGhurub: SemiDiameterBulanGhurub,
    val hoBulanGhurub: HoBulanGhurub,
    val refraksiHilal: RefraksiHilal,
    val tinggiBulanMariGhurub: TinggiBulanMariGhurub,
    val nishfulFadhlahBulan: NishfulFadhlahBulan,
    val parallaxNishfulFadhlah: ParallaxNishfulFadhlah,
    val setengahBusurSiangBulanHaqiqi: SetengahBusurSiangBulanHaqiqi,
    val setengahBusurSiangBulan: SetengahBusurSiangBulan,
    val lamaHilalMukuts: LamaHilalMukuts,
    val terbenamHilal: TerbenamHilal,
    val azimutMatahariGhurub: AzimutMatahariGhurub,
    val azimutBulanGhurub: AzimutBulanGhurub,
    val posisiHilal: PosisiHilal,
    val arahTerbenamHilal: ArahTerbenamHilal,
    val luasCahayaHilal: LuasCahayaHilal,
    val lebarNurulHilal: LebarNurulHilal,
    val kemiringanHilal: KemiringanHilal,
    val jarakBusurElongasi: JarakBusurElongasi,
    val kesimpulan: KesimpulanHisabHilal,
    val butirPerhitungan: List<ButirPerhitunganFalak>,
)
