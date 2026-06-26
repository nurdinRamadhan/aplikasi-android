package com.alhasanah.alhasanahmedia.domain.falak

import com.alhasanah.alhasanahmedia.data.model.falak.FalakEphemerisHarian
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class HisabHilalEphemerisCalculator {

    fun hitung(
        konteks: KonteksHisabHilal,
        ephemerisHarian: List<FalakEphemerisHarian>,
    ): HasilHisabHilalEphemeris {
        val ephemerisPerTanggal = ephemerisHarian.associateBy { LocalDate.parse(it.date) }
        val dataIjtima = tentukanDataIjtima(konteks, ephemerisPerTanggal)
        val sabaqIjtima = tentukanSabaqIjtima(dataIjtima)
        val saatIjtima = hitungSaatIjtima(konteks, dataIjtima, sabaqIjtima)
        val posisiMatahariHaqiqiGhurub = hitungPosisiMatahariHaqiqiGhurub(konteks, ephemerisPerTanggal)
        val sudutWaktuMatahariGhurub = hitungSudutWaktuMatahariGhurub(
            konteks = konteks,
            ephemerisPerTanggal = ephemerisPerTanggal,
            posisiMatahariHaqiqiGhurub = posisiMatahariHaqiqiGhurub
        )
        val koreksiWaktuDaerah = hitungKoreksiWaktuDaerah(konteks)
        val saatGhurub = hitungSaatGhurub(
            konteks = konteks,
            ephemerisPerTanggal = ephemerisPerTanggal,
            sudutWaktuMatahariGhurub = sudutWaktuMatahariGhurub,
            koreksiWaktuDaerah = koreksiWaktuDaerah
        )
        val asensiorektaMatahariGhurub = hitungAsensiorektaMatahariGhurub(konteks, ephemerisPerTanggal, saatGhurub)
        val asensiorektaBulanGhurub = hitungAsensiorektaBulanGhurub(konteks, ephemerisPerTanggal, saatGhurub)
        val sudutWaktuBulanGhurub = hitungSudutWaktuBulanGhurub(
            sudutWaktuMatahariGhurub = sudutWaktuMatahariGhurub,
            asensiorektaMatahariGhurub = asensiorektaMatahariGhurub,
            asensiorektaBulanGhurub = asensiorektaBulanGhurub
        )
        val deklinasiGhurub = hitungDeklinasiGhurub(konteks, ephemerisPerTanggal, saatGhurub)
        val tinggiBulanHaqiqiGhurub = hitungTinggiBulanHaqiqiGhurub(
            konteks = konteks,
            sudutWaktuBulanGhurub = sudutWaktuBulanGhurub,
            deklinasiGhurub = deklinasiGhurub
        )
        val parallaxBulanGhurub = hitungParallaxBulanGhurub(
            konteks = konteks,
            ephemerisPerTanggal = ephemerisPerTanggal,
            saatGhurub = saatGhurub,
            tinggiBulanHaqiqiGhurub = tinggiBulanHaqiqiGhurub
        )
        val semiDiameterBulanGhurub = hitungSemiDiameterBulanGhurub(konteks, ephemerisPerTanggal, saatGhurub)
        val hoBulanGhurub = hitungHoBulanGhurub(
            tinggiBulanHaqiqiGhurub = tinggiBulanHaqiqiGhurub,
            parallaxBulanGhurub = parallaxBulanGhurub,
            semiDiameterBulanGhurub = semiDiameterBulanGhurub
        )
        val refraksiHilal = hitungRefraksiHilal(hoBulanGhurub)
        val tinggiBulanMariGhurub = hitungTinggiBulanMariGhurub(
            posisiMatahariHaqiqiGhurub = posisiMatahariHaqiqiGhurub,
            hoBulanGhurub = hoBulanGhurub,
            refraksiHilal = refraksiHilal
        )
        val nishfulFadhlahBulan = hitungNishfulFadhlahBulan(konteks, deklinasiGhurub)
        val parallaxNishfulFadhlah = hitungParallaxNishfulFadhlah(
            nishfulFadhlahBulan = nishfulFadhlahBulan,
            parallaxBulanGhurub = parallaxBulanGhurub
        )
        val setengahBusurSiangBulanHaqiqi = hitungSetengahBusurSiangBulanHaqiqi(nishfulFadhlahBulan)
        val setengahBusurSiangBulan = hitungSetengahBusurSiangBulan(
            posisiMatahariHaqiqiGhurub = posisiMatahariHaqiqiGhurub,
            semiDiameterBulanGhurub = semiDiameterBulanGhurub,
            nishfulFadhlahBulan = nishfulFadhlahBulan,
            parallaxNishfulFadhlah = parallaxNishfulFadhlah,
            setengahBusurSiangBulanHaqiqi = setengahBusurSiangBulanHaqiqi
        )
        val lamaHilalMukuts = hitungLamaHilalMukuts(
            setengahBusurSiangBulan = setengahBusurSiangBulan,
            sudutWaktuBulanGhurub = sudutWaktuBulanGhurub
        )
        val terbenamHilal = hitungTerbenamHilal(saatGhurub, lamaHilalMukuts)
        val azimutMatahariGhurub = hitungAzimutMatahariGhurub(konteks, sudutWaktuMatahariGhurub)
        val azimutBulanGhurub = hitungAzimutBulanGhurub(konteks, sudutWaktuBulanGhurub, deklinasiGhurub)
        val posisiHilal = hitungPosisiHilal(azimutMatahariGhurub, azimutBulanGhurub)
        val arahTerbenamHilal = hitungArahTerbenamHilal(konteks, setengahBusurSiangBulan, deklinasiGhurub)
        val luasCahayaHilal = hitungLuasCahayaHilal(konteks, ephemerisPerTanggal, saatGhurub)
        val lebarNurulHilal = hitungLebarNurulHilal(posisiHilal, tinggiBulanMariGhurub)
        val kemiringanHilal = hitungKemiringanHilal(posisiHilal, tinggiBulanMariGhurub)
        val jarakBusurElongasi = hitungJarakBusurElongasi(
            asensiorektaMatahariGhurub = asensiorektaMatahariGhurub,
            asensiorektaBulanGhurub = asensiorektaBulanGhurub,
            deklinasiGhurub = deklinasiGhurub
        )
        val kesimpulan = susunKesimpulanHisabHilal(
            konteks = konteks,
            saatIjtima = saatIjtima,
            saatGhurub = saatGhurub,
            tinggiBulanMariGhurub = tinggiBulanMariGhurub,
            lamaHilalMukuts = lamaHilalMukuts,
            terbenamHilal = terbenamHilal,
            azimutMatahariGhurub = azimutMatahariGhurub,
            azimutBulanGhurub = azimutBulanGhurub,
            posisiHilal = posisiHilal,
            kemiringanHilal = kemiringanHilal,
            luasCahayaHilal = luasCahayaHilal,
            lebarNurulHilal = lebarNurulHilal,
            jarakBusurElongasi = jarakBusurElongasi,
            arahTerbenamHilal = arahTerbenamHilal
        )

        return HasilHisabHilalEphemeris(
            konteks = konteks,
            dataIjtima = dataIjtima,
            sabaqIjtima = sabaqIjtima,
            saatIjtima = saatIjtima,
            posisiMatahariHaqiqiGhurub = posisiMatahariHaqiqiGhurub,
            sudutWaktuMatahariGhurub = sudutWaktuMatahariGhurub,
            koreksiWaktuDaerah = koreksiWaktuDaerah,
            saatGhurub = saatGhurub,
            asensiorektaMatahariGhurub = asensiorektaMatahariGhurub,
            asensiorektaBulanGhurub = asensiorektaBulanGhurub,
            sudutWaktuBulanGhurub = sudutWaktuBulanGhurub,
            deklinasiGhurub = deklinasiGhurub,
            tinggiBulanHaqiqiGhurub = tinggiBulanHaqiqiGhurub,
            parallaxBulanGhurub = parallaxBulanGhurub,
            semiDiameterBulanGhurub = semiDiameterBulanGhurub,
            hoBulanGhurub = hoBulanGhurub,
            refraksiHilal = refraksiHilal,
            tinggiBulanMariGhurub = tinggiBulanMariGhurub,
            nishfulFadhlahBulan = nishfulFadhlahBulan,
            parallaxNishfulFadhlah = parallaxNishfulFadhlah,
            setengahBusurSiangBulanHaqiqi = setengahBusurSiangBulanHaqiqi,
            setengahBusurSiangBulan = setengahBusurSiangBulan,
            lamaHilalMukuts = lamaHilalMukuts,
            terbenamHilal = terbenamHilal,
            azimutMatahariGhurub = azimutMatahariGhurub,
            azimutBulanGhurub = azimutBulanGhurub,
            posisiHilal = posisiHilal,
            arahTerbenamHilal = arahTerbenamHilal,
            luasCahayaHilal = luasCahayaHilal,
            lebarNurulHilal = lebarNurulHilal,
            kemiringanHilal = kemiringanHilal,
            jarakBusurElongasi = jarakBusurElongasi,
            kesimpulan = kesimpulan,
            butirPerhitungan = listOf(
                tampilkanMarkaz(konteks),
                tampilkanDataIjtima(dataIjtima),
                tampilkanSabaqIjtima(dataIjtima, sabaqIjtima),
                tampilkanSaatIjtima(konteks, dataIjtima, sabaqIjtima, saatIjtima),
                tampilkanPosisiMatahariHaqiqiGhurub(konteks, posisiMatahariHaqiqiGhurub),
                tampilkanSudutWaktuMatahariGhurub(konteks, sudutWaktuMatahariGhurub),
                tampilkanKoreksiWaktuDaerah(koreksiWaktuDaerah),
                tampilkanSaatGhurub(saatGhurub),
                tampilkanAsensiorektaMatahariGhurub(asensiorektaMatahariGhurub),
                tampilkanAsensiorektaBulanGhurub(asensiorektaBulanGhurub),
                tampilkanSudutWaktuBulanGhurub(sudutWaktuBulanGhurub),
                tampilkanDeklinasiGhurub(deklinasiGhurub),
                tampilkanTinggiBulanHaqiqiGhurub(tinggiBulanHaqiqiGhurub),
                tampilkanParallaxBulanGhurub(parallaxBulanGhurub),
                tampilkanSemiDiameterBulanGhurub(semiDiameterBulanGhurub),
                tampilkanHoBulanGhurub(hoBulanGhurub),
                tampilkanRefraksiHilal(refraksiHilal),
                tampilkanTinggiBulanMariGhurub(tinggiBulanMariGhurub),
                tampilkanNishfulFadhlahBulan(nishfulFadhlahBulan),
                tampilkanParallaxNishfulFadhlah(parallaxNishfulFadhlah),
                tampilkanSetengahBusurSiangBulanHaqiqi(setengahBusurSiangBulanHaqiqi),
                tampilkanSetengahBusurSiangBulan(setengahBusurSiangBulan),
                tampilkanLamaHilalMukuts(lamaHilalMukuts),
                tampilkanTerbenamHilal(terbenamHilal),
                tampilkanAzimutMatahariGhurub(azimutMatahariGhurub),
                tampilkanAzimutBulanGhurub(azimutBulanGhurub),
                tampilkanPosisiHilal(posisiHilal),
                tampilkanArahTerbenamHilal(arahTerbenamHilal),
                tampilkanLuasCahayaHilal(luasCahayaHilal),
                tampilkanLebarNurulHilal(lebarNurulHilal),
                tampilkanKemiringanHilal(kemiringanHilal),
                tampilkanJarakBusurElongasi(jarakBusurElongasi),
            )
        )
    }

    private fun susunKesimpulanHisabHilal(
        konteks: KonteksHisabHilal,
        saatIjtima: SaatIjtima,
        saatGhurub: SaatGhurub,
        tinggiBulanMariGhurub: TinggiBulanMariGhurub,
        lamaHilalMukuts: LamaHilalMukuts,
        terbenamHilal: TerbenamHilal,
        azimutMatahariGhurub: AzimutMatahariGhurub,
        azimutBulanGhurub: AzimutBulanGhurub,
        posisiHilal: PosisiHilal,
        kemiringanHilal: KemiringanHilal,
        luasCahayaHilal: LuasCahayaHilal,
        lebarNurulHilal: LebarNurulHilal,
        jarakBusurElongasi: JarakBusurElongasi,
        arahTerbenamHilal: ArahTerbenamHilal,
    ): KesimpulanHisabHilal {
        val kriteria = konteks.kriteriaAwalBulan
        val ijtimaSebelumGhurub = saatIjtima.waktuLokal.tanggal.isBefore(saatGhurub.waktuLokal.tanggal) ||
            (saatIjtima.waktuLokal.tanggal == saatGhurub.waktuLokal.tanggal &&
                saatIjtima.waktuLokal.jamDesimal <= saatGhurub.waktuLokal.jamDesimal)
        val tinggiMemenuhi = kriteria.tinggiHilalMinimumDerajat?.let {
            tinggiBulanMariGhurub.tinggiBulanMariDerajat >= it
        }
        val elongasiMemenuhi = kriteria.elongasiMinimumDerajat?.let {
            jarakBusurElongasi.elongasiDerajat >= it
        }
        val memenuhiKriteria = listOfNotNull(
            if (kriteria.memakaiSyaratIjtimaSebelumGhurub) ijtimaSebelumGhurub else null,
            tinggiMemenuhi,
            elongasiMemenuhi
        ).all { it }
        val tanggalPrakiraan = if (memenuhiKriteria) {
            konteks.tanggalSituasiHilalMasehi.plusDays(1)
        } else {
            konteks.tanggalSituasiHilalMasehi.plusDays(2)
        }
        val tanggalPrakiraanTampil = formatTanggalLengkap(tanggalPrakiraan)
        val status = if (memenuhiKriteria) {
            "Memenuhi ${kriteria.nama}; awal ${konteks.bulanHijriah} diprakirakan jatuh pada $tanggalPrakiraanTampil."
        } else {
            "Belum memenuhi ${kriteria.nama}; awal ${konteks.bulanHijriah} diprakirakan setelah istikmal, pada $tanggalPrakiraanTampil."
        }
        val syarat = mutableListOf<String>()
        if (kriteria.memakaiSyaratIjtimaSebelumGhurub) {
            syarat += "ijtimak sebelum/saat ghurub=${if (ijtimaSebelumGhurub) "memenuhi" else "belum memenuhi"}"
        }
        kriteria.tinggiHilalMinimumDerajat?.let {
            syarat += "tinggi hilal mar'i tepi atas >= ${formatDerajat(it)}=${if (tinggiMemenuhi == true) "memenuhi" else "belum memenuhi"}"
        }
        kriteria.elongasiMinimumDerajat?.let {
            syarat += "elongasi geosentrik >= ${formatDerajat(it)}=${if (elongasiMemenuhi == true) "memenuhi" else "belum memenuhi"}"
        }

        return KesimpulanHisabHilal(
            bulanHijriah = konteks.bulanHijriah,
            kriteriaAwalBulan = kriteria,
            tanggalSituasiHilalMasehi = konteks.tanggalSituasiHilalMasehi,
            tanggalPrakiraanAwalBulanMasehi = tanggalPrakiraan,
            ijtima = saatIjtima.waktuLokal,
            ghurub = saatGhurub.waktuLokal,
            tinggiHilalMariDerajat = tinggiBulanMariGhurub.tinggiBulanMariDerajat,
            lamaHilalJam = lamaHilalMukuts.lamaHilalJam,
            terbenamHilal = terbenamHilal.waktuLokal,
            azimutMatahariDerajat = azimutMatahariGhurub.azimutDerajat,
            azimutHilalDerajat = azimutBulanGhurub.azimutDerajat,
            posisiHilalDerajat = posisiHilal.posisiHilalDerajat,
            posisiHilalDariMatahari = posisiHilal.arahDariMatahari,
            keadaanHilal = kemiringanHilal.keadaan,
            fractionIlluminationPersen = luasCahayaHilal.fibGhurub.hasil,
            cahayaHilalJari = lebarNurulHilal.nurulHilalJari,
            elongasiHilalDerajat = jarakBusurElongasi.elongasiDerajat,
            arahTerbenamHilalDerajat = arahTerbenamHilal.arahTerbenamDerajat,
            ijtimaSebelumGhurub = ijtimaSebelumGhurub,
            tinggiHilalMemenuhi = tinggiMemenuhi,
            elongasiMemenuhi = elongasiMemenuhi,
            memenuhiKriteria = memenuhiKriteria,
            statusPrakiraan = status,
            catatan = syarat.joinToString("; "),
        )
    }

    private fun tentukanDataIjtima(
        konteks: KonteksHisabHilal,
        ephemerisPerTanggal: Map<LocalDate, FalakEphemerisHarian>,
    ): DataIjtima {
        val tanggal = konteks.tanggalSituasiHilalMasehi
        val dataTanggal = ephemerisPerTanggal[tanggal] ?: error("Data ephemeris tanggal $tanggal tidak tersedia.")
        val barisFibTerkecil = dataTanggal.hourlyTable.moon.minByOrNull { row ->
            row.doubleAt("fraction_illumination_percent") ?: Double.POSITIVE_INFINITY
        } ?: error("Data Bulan tanggal $tanggal kosong.")
        val jamFibUt = barisFibTerkecil.hourUt()
        val bulanJamSetelahnya = barisEphemeris(ephemerisPerTanggal, tanggal, jamFibUt + 1, TabelEphemeris.BULAN)
        val matahariJamFib = barisEphemeris(ephemerisPerTanggal, tanggal, jamFibUt, TabelEphemeris.MATAHARI)
        val matahariJamSetelahnya = barisEphemeris(ephemerisPerTanggal, tanggal, jamFibUt + 1, TabelEphemeris.MATAHARI)

        return DataIjtima(
            fibTerkecilPersen = nilaiAngka(tanggal, jamFibUt, TabelEphemeris.BULAN, "fraction_illumination_percent", barisFibTerkecil),
            jamFibUt = jamFibUt,
            albJamFib = nilaiDerajat(tanggal, jamFibUt, TabelEphemeris.BULAN, "apparent_longitude", barisFibTerkecil),
            albJamSetelahnya = nilaiDerajat(bulanJamSetelahnya.tanggal, bulanJamSetelahnya.jamUt, TabelEphemeris.BULAN, "apparent_longitude", bulanJamSetelahnya.row),
            elmJamFib = nilaiDerajat(tanggal, jamFibUt, TabelEphemeris.MATAHARI, "apparent_ecliptic_longitude", matahariJamFib.row),
            elmJamSetelahnya = nilaiDerajat(matahariJamSetelahnya.tanggal, matahariJamSetelahnya.jamUt, TabelEphemeris.MATAHARI, "apparent_ecliptic_longitude", matahariJamSetelahnya.row),
        )
    }

    private fun tentukanSabaqIjtima(dataIjtima: DataIjtima): SabaqIjtima =
        SabaqIjtima(
            sabaqBulanDerajat = deltaMajuDerajat(dataIjtima.albJamFib.nilai, dataIjtima.albJamSetelahnya.nilai),
            sabaqMatahariDerajat = deltaMajuDerajat(dataIjtima.elmJamFib.nilai, dataIjtima.elmJamSetelahnya.nilai),
        )

    private fun hitungSaatIjtima(
        konteks: KonteksHisabHilal,
        dataIjtima: DataIjtima,
        sabaqIjtima: SabaqIjtima,
    ): SaatIjtima {
        val jarakElmAlb = selisihSudutBertanda(dataIjtima.elmJamFib.nilai, dataIjtima.albJamFib.nilai)
        val jamIjtimaUt = dataIjtima.jamFibUt + (
            jarakElmAlb / (sabaqIjtima.sabaqBulanDerajat - sabaqIjtima.sabaqMatahariDerajat)
            )
        return SaatIjtima(
            jarakElmAlbDerajat = jarakElmAlb,
            waktuUt = normalisasiWaktu(konteks.tanggalSituasiHilalMasehi, jamIjtimaUt, "GMT/UT"),
            waktuLokal = normalisasiWaktu(
                konteks.tanggalSituasiHilalMasehi,
                jamIjtimaUt + konteks.markaz.zonaWaktu.offsetJam,
                konteks.markaz.zonaWaktu.nama
            ),
        )
    }

    private fun hitungPosisiMatahariHaqiqiGhurub(
        konteks: KonteksHisabHilal,
        ephemerisPerTanggal: Map<LocalDate, FalakEphemerisHarian>,
    ): PosisiMatahariHaqiqiGhurub {
        val jamAcuanUt = floor(konteks.jamGhurubPerkiraanLokal - konteks.markaz.zonaWaktu.offsetJam).toInt()
        val matahariGhurub = barisEphemeris(
            ephemerisPerTanggal,
            konteks.tanggalSituasiHilalMasehi,
            jamAcuanUt,
            TabelEphemeris.MATAHARI
        )
        val semiDiameterMatahari = nilaiDerajat(
            matahariGhurub.tanggal,
            matahariGhurub.jamUt,
            TabelEphemeris.MATAHARI,
            "semi_diameter",
            matahariGhurub.row
        )
        val refraksiGhurub = 34.5 / 60.0
        val dip = sqrt(konteks.markaz.elevasiMeter.coerceAtLeast(0.0)) * 0.0293
        return PosisiMatahariHaqiqiGhurub(
            jamAcuanUt = jamAcuanUt,
            semiDiameterMatahariDerajat = semiDiameterMatahari,
            refraksiGhurubDerajat = refraksiGhurub,
            dipDerajat = dip,
            tinggiMatahariHaqiqiDerajat = 0.0 - semiDiameterMatahari.nilai - refraksiGhurub - dip,
        )
    }

    private fun hitungSudutWaktuMatahariGhurub(
        konteks: KonteksHisabHilal,
        ephemerisPerTanggal: Map<LocalDate, FalakEphemerisHarian>,
        posisiMatahariHaqiqiGhurub: PosisiMatahariHaqiqiGhurub,
    ): SudutWaktuMatahariGhurub {
        val matahariGhurub = barisEphemeris(
            ephemerisPerTanggal,
            konteks.tanggalSituasiHilalMasehi,
            posisiMatahariHaqiqiGhurub.jamAcuanUt,
            TabelEphemeris.MATAHARI
        )
        val deklinasiMatahari = nilaiDerajat(
            matahariGhurub.tanggal,
            matahariGhurub.jamUt,
            TabelEphemeris.MATAHARI,
            "apparent_declination",
            matahariGhurub.row
        )
        val lintang = konteks.markaz.lintangDerajat
        val deklinasi = deklinasiMatahari.nilai
        val tinggi = posisiMatahariHaqiqiGhurub.tinggiMatahariHaqiqiDerajat
        val argumenCosinus = -tanDeg(lintang) * tanDeg(deklinasi) +
            (sinDeg(tinggi) / cosDeg(lintang) / cosDeg(deklinasi))
        return SudutWaktuMatahariGhurub(
            jamAcuanUt = posisiMatahariHaqiqiGhurub.jamAcuanUt,
            deklinasiMatahariDerajat = deklinasiMatahari,
            tinggiMatahariHaqiqiDerajat = tinggi,
            argumenCosinus = argumenCosinus,
            sudutWaktuDerajat = acos(argumenCosinus.coerceIn(-1.0, 1.0)) * 180.0 / PI,
        )
    }

    private fun hitungKoreksiWaktuDaerah(konteks: KonteksHisabHilal): KoreksiWaktuDaerah =
        KoreksiWaktuDaerah(
            bujurStandarDerajat = konteks.markaz.zonaWaktu.bujurStandarDerajat,
            bujurMarkazDerajat = konteks.markaz.bujurDerajat,
            koreksiJam = (konteks.markaz.zonaWaktu.bujurStandarDerajat - konteks.markaz.bujurDerajat) / 15.0,
        )

    private fun hitungSaatGhurub(
        konteks: KonteksHisabHilal,
        ephemerisPerTanggal: Map<LocalDate, FalakEphemerisHarian>,
        sudutWaktuMatahariGhurub: SudutWaktuMatahariGhurub,
        koreksiWaktuDaerah: KoreksiWaktuDaerah,
    ): SaatGhurub {
        val matahariGhurub = barisEphemeris(
            ephemerisPerTanggal,
            konteks.tanggalSituasiHilalMasehi,
            sudutWaktuMatahariGhurub.jamAcuanUt,
            TabelEphemeris.MATAHARI
        )
        val equationOfTime = nilaiJam(
            matahariGhurub.tanggal,
            matahariGhurub.jamUt,
            TabelEphemeris.MATAHARI,
            "equation_of_time",
            matahariGhurub.row
        )
        val jamGhurub = (sudutWaktuMatahariGhurub.sudutWaktuDerajat / 15.0) +
            (12.0 - equationOfTime.nilai) +
            koreksiWaktuDaerah.koreksiJam
        return SaatGhurub(
            sudutWaktuMatahariDerajat = sudutWaktuMatahariGhurub.sudutWaktuDerajat,
            equationOfTimeJam = equationOfTime,
            koreksiWaktuDaerahJam = koreksiWaktuDaerah.koreksiJam,
            waktuLokal = normalisasiWaktu(konteks.tanggalSituasiHilalMasehi, jamGhurub, konteks.markaz.zonaWaktu.nama),
        )
    }

    private fun hitungAsensiorektaMatahariGhurub(
        konteks: KonteksHisabHilal,
        ephemerisPerTanggal: Map<LocalDate, FalakEphemerisHarian>,
        saatGhurub: SaatGhurub,
    ): AsensiorektaMatahariGhurub =
        AsensiorektaMatahariGhurub(
            interpolasi = interpolasiGhurub(
                konteks = konteks,
                ephemerisPerTanggal = ephemerisPerTanggal,
                saatGhurub = saatGhurub,
                tabel = TabelEphemeris.MATAHARI,
                kolom = "apparent_right_ascension",
                mode = ModeInterpolasi.SUDUT_MAJU
            )
        )

    private fun hitungAsensiorektaBulanGhurub(
        konteks: KonteksHisabHilal,
        ephemerisPerTanggal: Map<LocalDate, FalakEphemerisHarian>,
        saatGhurub: SaatGhurub,
    ): AsensiorektaBulanGhurub =
        AsensiorektaBulanGhurub(
            interpolasi = interpolasiGhurub(
                konteks = konteks,
                ephemerisPerTanggal = ephemerisPerTanggal,
                saatGhurub = saatGhurub,
                tabel = TabelEphemeris.BULAN,
                kolom = "apparent_right_ascension",
                mode = ModeInterpolasi.SUDUT_MAJU
            )
        )

    private fun hitungSudutWaktuBulanGhurub(
        sudutWaktuMatahariGhurub: SudutWaktuMatahariGhurub,
        asensiorektaMatahariGhurub: AsensiorektaMatahariGhurub,
        asensiorektaBulanGhurub: AsensiorektaBulanGhurub,
    ): SudutWaktuBulanGhurub {
        val arMatahari = asensiorektaMatahariGhurub.interpolasi.hasilDerajat
        val arBulan = asensiorektaBulanGhurub.interpolasi.hasilDerajat
        return SudutWaktuBulanGhurub(
            asensiorektaMatahariDerajat = arMatahari,
            asensiorektaBulanDerajat = arBulan,
            sudutWaktuMatahariDerajat = sudutWaktuMatahariGhurub.sudutWaktuDerajat,
            sudutWaktuBulanDerajat = selisihSudutBertanda(arMatahari, arBulan) + sudutWaktuMatahariGhurub.sudutWaktuDerajat,
        )
    }

    private fun hitungDeklinasiGhurub(
        konteks: KonteksHisabHilal,
        ephemerisPerTanggal: Map<LocalDate, FalakEphemerisHarian>,
        saatGhurub: SaatGhurub,
    ): DeklinasiGhurub =
        DeklinasiGhurub(
            matahari = interpolasiGhurub(
                konteks = konteks,
                ephemerisPerTanggal = ephemerisPerTanggal,
                saatGhurub = saatGhurub,
                tabel = TabelEphemeris.MATAHARI,
                kolom = "apparent_declination",
                mode = ModeInterpolasi.LINEAR
            ),
            bulan = interpolasiGhurub(
                konteks = konteks,
                ephemerisPerTanggal = ephemerisPerTanggal,
                saatGhurub = saatGhurub,
                tabel = TabelEphemeris.BULAN,
                kolom = "apparent_declination",
                mode = ModeInterpolasi.LINEAR
            )
        )

    private fun hitungTinggiBulanHaqiqiGhurub(
        konteks: KonteksHisabHilal,
        sudutWaktuBulanGhurub: SudutWaktuBulanGhurub,
        deklinasiGhurub: DeklinasiGhurub,
    ): TinggiBulanHaqiqiGhurub {
        val lintang = konteks.markaz.lintangDerajat
        val deklinasiBulan = deklinasiGhurub.bulan.hasilDerajat
        val sudutWaktuBulan = sudutWaktuBulanGhurub.sudutWaktuBulanDerajat
        val argumenSinus = sinDeg(lintang) * sinDeg(deklinasiBulan) +
            cosDeg(lintang) * cosDeg(deklinasiBulan) * cosDeg(sudutWaktuBulan)
        return TinggiBulanHaqiqiGhurub(
            lintangMarkazDerajat = lintang,
            deklinasiBulanDerajat = deklinasiBulan,
            sudutWaktuBulanDerajat = sudutWaktuBulan,
            argumenSinus = argumenSinus,
            tinggiBulanHaqiqiDerajat = kotlin.math.asin(argumenSinus.coerceIn(-1.0, 1.0)) * 180.0 / PI,
        )
    }

    private fun hitungParallaxBulanGhurub(
        konteks: KonteksHisabHilal,
        ephemerisPerTanggal: Map<LocalDate, FalakEphemerisHarian>,
        saatGhurub: SaatGhurub,
        tinggiBulanHaqiqiGhurub: TinggiBulanHaqiqiGhurub,
    ): ParallaxBulanGhurub {
        val hp = interpolasiGhurub(
            konteks = konteks,
            ephemerisPerTanggal = ephemerisPerTanggal,
            saatGhurub = saatGhurub,
            tabel = TabelEphemeris.BULAN,
            kolom = "horizontal_parallax",
            mode = ModeInterpolasi.LINEAR
        )
        return ParallaxBulanGhurub(
            horizontalParallax = hp,
            tinggiBulanHaqiqiDerajat = tinggiBulanHaqiqiGhurub.tinggiBulanHaqiqiDerajat,
            parallaxDerajat = hp.hasilDerajat * cosDeg(tinggiBulanHaqiqiGhurub.tinggiBulanHaqiqiDerajat),
        )
    }

    private fun hitungSemiDiameterBulanGhurub(
        konteks: KonteksHisabHilal,
        ephemerisPerTanggal: Map<LocalDate, FalakEphemerisHarian>,
        saatGhurub: SaatGhurub,
    ): SemiDiameterBulanGhurub =
        SemiDiameterBulanGhurub(
            interpolasi = interpolasiGhurub(
                konteks = konteks,
                ephemerisPerTanggal = ephemerisPerTanggal,
                saatGhurub = saatGhurub,
                tabel = TabelEphemeris.BULAN,
                kolom = "semi_diameter",
                mode = ModeInterpolasi.LINEAR
            )
        )

    private fun hitungHoBulanGhurub(
        tinggiBulanHaqiqiGhurub: TinggiBulanHaqiqiGhurub,
        parallaxBulanGhurub: ParallaxBulanGhurub,
        semiDiameterBulanGhurub: SemiDiameterBulanGhurub,
    ): HoBulanGhurub =
        HoBulanGhurub(
            tinggiBulanHaqiqiDerajat = tinggiBulanHaqiqiGhurub.tinggiBulanHaqiqiDerajat,
            parallaxDerajat = parallaxBulanGhurub.parallaxDerajat,
            semiDiameterBulanDerajat = semiDiameterBulanGhurub.interpolasi.hasilDerajat,
            hoDerajat = tinggiBulanHaqiqiGhurub.tinggiBulanHaqiqiDerajat -
                parallaxBulanGhurub.parallaxDerajat +
                semiDiameterBulanGhurub.interpolasi.hasilDerajat,
        )

    private fun hitungRefraksiHilal(hoBulanGhurub: HoBulanGhurub): RefraksiHilal {
        val ho = hoBulanGhurub.hoDerajat
        val refraksi = if (ho <= 0.0) {
            34.5 / 60.0
        } else {
            0.0167 / tanDeg(ho + 7.31 / (ho + 4.4))
        }
        return RefraksiHilal(
            hoDerajat = ho,
            refraksiDerajat = refraksi,
            menggunakanRefraksiRataRata = ho <= 0.0,
        )
    }

    private fun hitungTinggiBulanMariGhurub(
        posisiMatahariHaqiqiGhurub: PosisiMatahariHaqiqiGhurub,
        hoBulanGhurub: HoBulanGhurub,
        refraksiHilal: RefraksiHilal,
    ): TinggiBulanMariGhurub =
        TinggiBulanMariGhurub(
            hoDerajat = hoBulanGhurub.hoDerajat,
            refraksiDerajat = refraksiHilal.refraksiDerajat,
            dipDerajat = posisiMatahariHaqiqiGhurub.dipDerajat,
            tinggiBulanMariDerajat = hoBulanGhurub.hoDerajat +
                refraksiHilal.refraksiDerajat +
                posisiMatahariHaqiqiGhurub.dipDerajat,
        )

    private fun hitungNishfulFadhlahBulan(
        konteks: KonteksHisabHilal,
        deklinasiGhurub: DeklinasiGhurub,
    ): NishfulFadhlahBulan {
        val lintang = konteks.markaz.lintangDerajat
        val deklinasiBulan = deklinasiGhurub.bulan.hasilDerajat
        val argumenSinus = (sinDeg(lintang) * sinDeg(deklinasiBulan)) / (cosDeg(lintang) * cosDeg(deklinasiBulan))
        return NishfulFadhlahBulan(
            lintangMarkazDerajat = lintang,
            deklinasiBulanDerajat = deklinasiBulan,
            argumenSinus = argumenSinus,
            nfDerajat = kotlin.math.asin(argumenSinus.coerceIn(-1.0, 1.0)) * 180.0 / PI,
        )
    }

    private fun hitungParallaxNishfulFadhlah(
        nishfulFadhlahBulan: NishfulFadhlahBulan,
        parallaxBulanGhurub: ParallaxBulanGhurub,
    ): ParallaxNishfulFadhlah =
        ParallaxNishfulFadhlah(
            nfDerajat = nishfulFadhlahBulan.nfDerajat,
            horizontalParallaxDerajat = parallaxBulanGhurub.horizontalParallax.hasilDerajat,
            pnfDerajat = cosDeg(nishfulFadhlahBulan.nfDerajat) * parallaxBulanGhurub.horizontalParallax.hasilDerajat,
        )

    private fun hitungSetengahBusurSiangBulanHaqiqi(
        nishfulFadhlahBulan: NishfulFadhlahBulan,
    ): SetengahBusurSiangBulanHaqiqi =
        SetengahBusurSiangBulanHaqiqi(
            nfDerajat = nishfulFadhlahBulan.nfDerajat,
            sbshDerajat = 90.0 + nishfulFadhlahBulan.nfDerajat,
        )

    private fun hitungSetengahBusurSiangBulan(
        posisiMatahariHaqiqiGhurub: PosisiMatahariHaqiqiGhurub,
        semiDiameterBulanGhurub: SemiDiameterBulanGhurub,
        nishfulFadhlahBulan: NishfulFadhlahBulan,
        parallaxNishfulFadhlah: ParallaxNishfulFadhlah,
        setengahBusurSiangBulanHaqiqi: SetengahBusurSiangBulanHaqiqi,
    ): SetengahBusurSiangBulan {
        val sd = semiDiameterBulanGhurub.interpolasi.hasilDerajat
        val refraksiRataRata = 34.5 / 60.0
        val dip = posisiMatahariHaqiqiGhurub.dipDerajat
        val koreksiTepiAtas = sd + refraksiRataRata + dip
        val sbsh = setengahBusurSiangBulanHaqiqi.sbshDerajat
        val sbs = if (sbsh > 90.0) {
            90.0 + nishfulFadhlahBulan.nfDerajat - parallaxNishfulFadhlah.pnfDerajat + koreksiTepiAtas
        } else {
            90.0 + nishfulFadhlahBulan.nfDerajat + parallaxNishfulFadhlah.pnfDerajat - koreksiTepiAtas
        }
        return SetengahBusurSiangBulan(
            sbshDerajat = sbsh,
            nfDerajat = nishfulFadhlahBulan.nfDerajat,
            pnfDerajat = parallaxNishfulFadhlah.pnfDerajat,
            semiDiameterBulanDerajat = sd,
            refraksiRataRataDerajat = refraksiRataRata,
            dipDerajat = dip,
            menggunakanRumusSbshLebihDari90 = sbsh > 90.0,
            sbsDerajat = sbs,
        )
    }

    private fun hitungLamaHilalMukuts(
        setengahBusurSiangBulan: SetengahBusurSiangBulan,
        sudutWaktuBulanGhurub: SudutWaktuBulanGhurub,
    ): LamaHilalMukuts =
        LamaHilalMukuts(
            sbsDerajat = setengahBusurSiangBulan.sbsDerajat,
            sudutWaktuBulanDerajat = sudutWaktuBulanGhurub.sudutWaktuBulanDerajat,
            lamaHilalJam = (setengahBusurSiangBulan.sbsDerajat - sudutWaktuBulanGhurub.sudutWaktuBulanDerajat) / 15.0,
        )

    private fun hitungTerbenamHilal(
        saatGhurub: SaatGhurub,
        lamaHilalMukuts: LamaHilalMukuts,
    ): TerbenamHilal =
        TerbenamHilal(
            ghurub = saatGhurub.waktuLokal,
            lamaHilalJam = lamaHilalMukuts.lamaHilalJam,
            waktuLokal = normalisasiWaktu(
                saatGhurub.waktuLokal.tanggal,
                saatGhurub.waktuLokal.jamDesimal + lamaHilalMukuts.lamaHilalJam,
                saatGhurub.waktuLokal.zona
            ),
        )

    private fun hitungAzimutMatahariGhurub(
        konteks: KonteksHisabHilal,
        sudutWaktuMatahariGhurub: SudutWaktuMatahariGhurub,
    ): AzimutMatahariGhurub {
        val lintang = konteks.markaz.lintangDerajat
        val sudutWaktu = sudutWaktuMatahariGhurub.sudutWaktuDerajat
        val deklinasi = sudutWaktuMatahariGhurub.deklinasiMatahariDerajat.nilai
        val argumenTangen = (-sinDeg(lintang) / tanDeg(sudutWaktu)) +
            (cosDeg(lintang) * tanDeg(deklinasi) / sinDeg(sudutWaktu))
        val azimut = atan(argumenTangen) * 180.0 / PI
        return AzimutMatahariGhurub(
            lintangMarkazDerajat = lintang,
            sudutWaktuMatahariDerajat = sudutWaktu,
            deklinasiMatahariDerajat = deklinasi,
            argumenTangen = argumenTangen,
            azimutDerajat = azimut,
            arahDariBarat = arahAzimutDariBarat(azimut),
        )
    }

    private fun hitungAzimutBulanGhurub(
        konteks: KonteksHisabHilal,
        sudutWaktuBulanGhurub: SudutWaktuBulanGhurub,
        deklinasiGhurub: DeklinasiGhurub,
    ): AzimutBulanGhurub {
        val lintang = konteks.markaz.lintangDerajat
        val sudutWaktu = sudutWaktuBulanGhurub.sudutWaktuBulanDerajat
        val deklinasi = deklinasiGhurub.bulan.hasilDerajat
        val argumenTangen = (-sinDeg(lintang) / tanDeg(sudutWaktu)) +
            (cosDeg(lintang) * tanDeg(deklinasi) / sinDeg(sudutWaktu))
        val azimut = atan(argumenTangen) * 180.0 / PI
        return AzimutBulanGhurub(
            lintangMarkazDerajat = lintang,
            sudutWaktuBulanDerajat = sudutWaktu,
            deklinasiBulanDerajat = deklinasi,
            argumenTangen = argumenTangen,
            azimutDerajat = azimut,
            arahDariBarat = arahAzimutDariBarat(azimut),
        )
    }

    private fun hitungPosisiHilal(
        azimutMatahariGhurub: AzimutMatahariGhurub,
        azimutBulanGhurub: AzimutBulanGhurub,
    ): PosisiHilal {
        val posisi = azimutBulanGhurub.azimutDerajat - azimutMatahariGhurub.azimutDerajat
        return PosisiHilal(
            azimutBulanDerajat = azimutBulanGhurub.azimutDerajat,
            azimutMatahariDerajat = azimutMatahariGhurub.azimutDerajat,
            posisiHilalDerajat = posisi,
            arahDariMatahari = if (posisi >= 0.0) "utara Matahari" else "selatan Matahari",
        )
    }

    private fun hitungArahTerbenamHilal(
        konteks: KonteksHisabHilal,
        setengahBusurSiangBulan: SetengahBusurSiangBulan,
        deklinasiGhurub: DeklinasiGhurub,
    ): ArahTerbenamHilal {
        val lintang = konteks.markaz.lintangDerajat
        val sbs = setengahBusurSiangBulan.sbsDerajat
        val deklinasi = deklinasiGhurub.bulan.hasilDerajat
        val argumenTangen = (-sinDeg(lintang) / tanDeg(sbs)) +
            (cosDeg(lintang) * tanDeg(deklinasi) / sinDeg(sbs))
        return ArahTerbenamHilal(
            lintangMarkazDerajat = lintang,
            sbsDerajat = sbs,
            deklinasiBulanDerajat = deklinasi,
            argumenTangen = argumenTangen,
            arahTerbenamDerajat = atan(argumenTangen) * 180.0 / PI,
        )
    }

    private fun hitungLuasCahayaHilal(
        konteks: KonteksHisabHilal,
        ephemerisPerTanggal: Map<LocalDate, FalakEphemerisHarian>,
        saatGhurub: SaatGhurub,
    ): LuasCahayaHilal =
        LuasCahayaHilal(
            fibGhurub = interpolasiAngkaGhurub(
                konteks = konteks,
                ephemerisPerTanggal = ephemerisPerTanggal,
                saatGhurub = saatGhurub,
                tabel = TabelEphemeris.BULAN,
                kolom = "fraction_illumination_percent",
                satuan = "persen"
            )
        )

    private fun hitungLebarNurulHilal(
        posisiHilal: PosisiHilal,
        tinggiBulanMariGhurub: TinggiBulanMariGhurub,
    ): LebarNurulHilal =
        LebarNurulHilal(
            posisiHilalDerajat = posisiHilal.posisiHilalDerajat,
            tinggiBulanMariDerajat = tinggiBulanMariGhurub.tinggiBulanMariDerajat,
            nurulHilalJari = sqrt(
                posisiHilal.posisiHilalDerajat * posisiHilal.posisiHilalDerajat +
                    tinggiBulanMariGhurub.tinggiBulanMariDerajat * tinggiBulanMariGhurub.tinggiBulanMariDerajat
            ) / 15.0,
        )

    private fun hitungKemiringanHilal(
        posisiHilal: PosisiHilal,
        tinggiBulanMariGhurub: TinggiBulanMariGhurub,
    ): KemiringanHilal {
        val kemiringan = kotlin.math.abs(
            atan(posisiHilal.posisiHilalDerajat / tinggiBulanMariGhurub.tinggiBulanMariDerajat) * 180.0 / PI
        )
        val keadaan = if (kemiringan < 15.0) {
            "hilal terlentang"
        } else if (posisiHilal.posisiHilalDerajat >= 0.0) {
            "hilal miring ke Utara"
        } else {
            "hilal miring ke Selatan"
        }
        return KemiringanHilal(
            posisiHilalDerajat = posisiHilal.posisiHilalDerajat,
            tinggiBulanMariDerajat = tinggiBulanMariGhurub.tinggiBulanMariDerajat,
            kemiringanDerajat = kemiringan,
            keadaan = keadaan,
        )
    }

    private fun hitungJarakBusurElongasi(
        asensiorektaMatahariGhurub: AsensiorektaMatahariGhurub,
        asensiorektaBulanGhurub: AsensiorektaBulanGhurub,
        deklinasiGhurub: DeklinasiGhurub,
    ): JarakBusurElongasi {
        val deklinasiMatahari = deklinasiGhurub.matahari.hasilDerajat
        val deklinasiBulan = deklinasiGhurub.bulan.hasilDerajat
        val arMatahari = asensiorektaMatahariGhurub.interpolasi.hasilDerajat
        val arBulan = asensiorektaBulanGhurub.interpolasi.hasilDerajat
        val argumenCosinus = sinDeg(deklinasiMatahari) * sinDeg(deklinasiBulan) +
            cosDeg(deklinasiMatahari) * cosDeg(deklinasiBulan) * cosDeg(arMatahari - arBulan)
        return JarakBusurElongasi(
            deklinasiMatahariDerajat = deklinasiMatahari,
            deklinasiBulanDerajat = deklinasiBulan,
            asensiorektaMatahariDerajat = arMatahari,
            asensiorektaBulanDerajat = arBulan,
            argumenCosinus = argumenCosinus,
            elongasiDerajat = acos(argumenCosinus.coerceIn(-1.0, 1.0)) * 180.0 / PI,
        )
    }

    // 01. Markaz
    private fun tampilkanMarkaz(konteks: KonteksHisabHilal): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 1,
            judul = "Markaz",
            rumus = "Markaz = nama tempat, lintang (phi), bujur (lambda), dan tinggi tempat/elevasi.",
            substitusi = "${konteks.markaz.nama}; phi=${formatDerajat(konteks.markaz.lintangDerajat)}, lambda=${formatDerajat(konteks.markaz.bujurDerajat)}, elevasi=${formatAngka(konteks.markaz.elevasiMeter)} m",
            hasil = "${konteks.markaz.nama}, ${konteks.markaz.zonaWaktu.nama}",
            catatan = "Nilai ini berasal dari input pengguna, pilihan peta, GPS, atau markaz tersimpan."
        )

    // 02. Data ijtimak: FIB, ALB, ELM
    private fun tampilkanDataIjtima(dataIjtima: DataIjtima): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 2,
            judul = "Tentukan nilai-nilai FIB, ALB, dan ELM",
            rumus = "Ambil Illuminasi Bulan / FIB terkecil, lalu ambil ALB dan ELM pada jam FIB dan jam setelahnya.",
            substitusi = "FIB=${formatAngka(dataIjtima.fibTerkecilPersen.nilai)}% pada jam ${dataIjtima.jamFibUt} GMT/UT; ALB=${formatDerajat(dataIjtima.albJamFib.nilai)} -> ${formatDerajat(dataIjtima.albJamSetelahnya.nilai)}; ELM=${formatDerajat(dataIjtima.elmJamFib.nilai)} -> ${formatDerajat(dataIjtima.elmJamSetelahnya.nilai)}",
            hasil = "Jam FIB terkecil: ${dataIjtima.jamFibUt} GMT/UT",
            sumber = listOf(dataIjtima.fibTerkecilPersen.sumber, dataIjtima.albJamFib.sumber, dataIjtima.albJamSetelahnya.sumber, dataIjtima.elmJamFib.sumber, dataIjtima.elmJamSetelahnya.sumber)
        )

    // 03. Sabaq ijtimak: SB dan SM
    private fun tampilkanSabaqIjtima(
        dataIjtima: DataIjtima,
        sabaqIjtima: SabaqIjtima,
    ): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 3,
            judul = "Sabaq Bulan (SB) dan Sabaq Matahari (SM) saat Ijtimak",
            rumus = "SB = selisih antara dua nilai ALB; SM = selisih antara dua nilai ELM.",
            substitusi = "SB=${formatDerajat(dataIjtima.albJamSetelahnya.nilai)} - ${formatDerajat(dataIjtima.albJamFib.nilai)}; SM=${formatDerajat(dataIjtima.elmJamSetelahnya.nilai)} - ${formatDerajat(dataIjtima.elmJamFib.nilai)}",
            hasil = "SB=${formatDerajat(sabaqIjtima.sabaqBulanDerajat)}, SM=${formatDerajat(sabaqIjtima.sabaqMatahariDerajat)}",
            sumber = listOf(dataIjtima.albJamFib.sumber, dataIjtima.albJamSetelahnya.sumber, dataIjtima.elmJamFib.sumber, dataIjtima.elmJamSetelahnya.sumber)
        )

    // 04. Saat ijtimak
    private fun tampilkanSaatIjtima(
        konteks: KonteksHisabHilal,
        dataIjtima: DataIjtima,
        sabaqIjtima: SabaqIjtima,
        saatIjtima: SaatIjtima,
    ): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 4,
            judul = "Saat Ijtimak",
            rumus = "Ijtimak = Jam FIB terkecil + (ELM - ALB) : (SB - SM) + selisih zona waktu.",
            substitusi = "${dataIjtima.jamFibUt} + (${formatDerajat(dataIjtima.elmJamFib.nilai)} - ${formatDerajat(dataIjtima.albJamFib.nilai)}) : (${formatDerajat(sabaqIjtima.sabaqBulanDerajat)} - ${formatDerajat(sabaqIjtima.sabaqMatahariDerajat)}) + ${formatAngka(konteks.markaz.zonaWaktu.offsetJam)} jam",
            hasil = "${formatJam(saatIjtima.waktuLokal.jamDesimal)} ${saatIjtima.waktuLokal.zona}, ${saatIjtima.waktuLokal.tanggal}",
            catatan = "Selisih ELM-ALB dinormalisasi sebagai selisih sudut bertanda terdekat: ${formatDerajat(saatIjtima.jarakElmAlbDerajat)}."
        )

    // 05. Posisi Matahari haqiqi ghurub
    private fun tampilkanPosisiMatahariHaqiqiGhurub(
        konteks: KonteksHisabHilal,
        posisi: PosisiMatahariHaqiqiGhurub,
    ): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 5,
            judul = "Posisi Matahari Haqiqi pada jam ghurub",
            rumus = "h matahari = 0° - Sd matahari - refraksi ghurub - dip.",
            substitusi = "0° - ${formatDerajat(posisi.semiDiameterMatahariDerajat.nilai)} - ${formatDerajat(posisi.refraksiGhurubDerajat)} - ${formatDerajat(posisi.dipDerajat)}",
            hasil = formatDerajat(posisi.tinggiMatahariHaqiqiDerajat),
            catatan = "Sd matahari diambil pada jam ${posisi.jamAcuanUt} GMT/UT, yaitu perkiraan ghurub ${formatJam(konteks.jamGhurubPerkiraanLokal)} ${konteks.markaz.zonaWaktu.nama}. Dip = sqrt(elevasi) x 0,0293.",
            sumber = listOf(posisi.semiDiameterMatahariDerajat.sumber)
        )

    // 06. Sudut waktu Matahari ghurub
    private fun tampilkanSudutWaktuMatahariGhurub(
        konteks: KonteksHisabHilal,
        sudutWaktu: SudutWaktuMatahariGhurub,
    ): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 6,
            judul = "Sudut Waktu Matahari pada jam Ghurub",
            rumus = "t matahari = cos^-1(-tan phi x tan d matahari + sin h matahari / cos phi / cos d matahari).",
            substitusi = "cos^-1(-tan ${formatDerajat(konteks.markaz.lintangDerajat)} x tan ${formatDerajat(sudutWaktu.deklinasiMatahariDerajat.nilai)} + sin ${formatDerajat(sudutWaktu.tinggiMatahariHaqiqiDerajat)} / cos ${formatDerajat(konteks.markaz.lintangDerajat)} / cos ${formatDerajat(sudutWaktu.deklinasiMatahariDerajat.nilai)})",
            hasil = formatDerajat(sudutWaktu.sudutWaktuDerajat),
            catatan = "Deklinasi Matahari diambil dari Apparent Declination jam ${sudutWaktu.jamAcuanUt} GMT/UT. Argumen cosinus: ${formatAngka(sudutWaktu.argumenCosinus)}.",
            sumber = listOf(sudutWaktu.deklinasiMatahariDerajat.sumber)
        )

    // 07. Koreksi waktu daerah
    private fun tampilkanKoreksiWaktuDaerah(koreksi: KoreksiWaktuDaerah): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 7,
            judul = "Koreksi Waktu Daerah Kota",
            rumus = "KWD = (Bujur standar - Bujur markaz) : 15.",
            substitusi = "(${formatDerajat(koreksi.bujurStandarDerajat)} - ${formatDerajat(koreksi.bujurMarkazDerajat)}) : 15",
            hasil = "${formatAngka(koreksi.koreksiJam)} jam",
        )

    // 08. Saat ghurub
    private fun tampilkanSaatGhurub(saatGhurub: SaatGhurub): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 8,
            judul = "Ghurub / Saat Matahari Terbenam",
            rumus = "Ghurub = (t matahari : 15) + (12 - equation of time) + KWD.",
            substitusi = "(${formatDerajat(saatGhurub.sudutWaktuMatahariDerajat)} : 15) + (12 - ${formatAngka(saatGhurub.equationOfTimeJam.nilai)}) + ${formatAngka(saatGhurub.koreksiWaktuDaerahJam)}",
            hasil = "${formatJam(saatGhurub.waktuLokal.jamDesimal)} ${saatGhurub.waktuLokal.zona}, ${saatGhurub.waktuLokal.tanggal}",
            sumber = listOf(saatGhurub.equationOfTimeJam.sumber)
        )

    // 09. Asensiorekta Matahari ghurub
    private fun tampilkanAsensiorektaMatahariGhurub(data: AsensiorektaMatahariGhurub): ButirPerhitunganFalak =
        tampilkanInterpolasi(
            nomor = 9,
            judul = "AR Matahari pada waktu Ghurub",
            rumus = "AR matahari ghurub = Na - (Na - Nb) x Nc.",
            interpolasi = data.interpolasi
        )

    // 10. Asensiorekta Bulan ghurub
    private fun tampilkanAsensiorektaBulanGhurub(data: AsensiorektaBulanGhurub): ButirPerhitunganFalak =
        tampilkanInterpolasi(
            nomor = 10,
            judul = "AR Bulan pada waktu Ghurub",
            rumus = "AR bulan ghurub = Na - (Na - Nb) x Nc.",
            interpolasi = data.interpolasi
        )

    // 11. Sudut waktu Bulan ghurub
    private fun tampilkanSudutWaktuBulanGhurub(data: SudutWaktuBulanGhurub): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 11,
            judul = "Sudut Waktu Bulan pada jam Ghurub",
            rumus = "t bulan = (AR matahari - AR bulan) + t matahari.",
            substitusi = "(${formatDerajat(data.asensiorektaMatahariDerajat)} - ${formatDerajat(data.asensiorektaBulanDerajat)}) + ${formatDerajat(data.sudutWaktuMatahariDerajat)}",
            hasil = formatDerajat(data.sudutWaktuBulanDerajat),
            catatan = "Selisih AR matahari dan AR bulan dinormalisasi sebagai selisih sudut bertanda terdekat agar aman saat melewati 0°/360°."
        )

    // 12. Deklinasi Matahari dan Bulan ghurub
    private fun tampilkanDeklinasiGhurub(data: DeklinasiGhurub): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 12,
            judul = "Deklinasi Matahari dan Bulan pada jam Ghurub",
            rumus = "Deklinasi ghurub = Na - (Na - Nb) x Nc.",
            substitusi = "D matahari: ${substitusiInterpolasi(data.matahari)}; D bulan: ${substitusiInterpolasi(data.bulan)}",
            hasil = "D matahari=${formatDerajat(data.matahari.hasilDerajat)}, D bulan=${formatDerajat(data.bulan.hasilDerajat)}",
            sumber = listOf(data.matahari.nilaiAtas.sumber, data.matahari.nilaiBawah.sumber, data.bulan.nilaiAtas.sumber, data.bulan.nilaiBawah.sumber)
        )

    // 13. Tinggi Bulan haqiqi ghurub
    private fun tampilkanTinggiBulanHaqiqiGhurub(data: TinggiBulanHaqiqiGhurub): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 13,
            judul = "Tinggi Bulan Haqiqi pada jam Ghurub",
            rumus = "h bulan = sin^-1(sin phi x sin d bulan + cos phi x cos d bulan x cos t bulan).",
            substitusi = "sin^-1(sin ${formatDerajat(data.lintangMarkazDerajat)} x sin ${formatDerajat(data.deklinasiBulanDerajat)} + cos ${formatDerajat(data.lintangMarkazDerajat)} x cos ${formatDerajat(data.deklinasiBulanDerajat)} x cos ${formatDerajat(data.sudutWaktuBulanDerajat)})",
            hasil = formatDerajat(data.tinggiBulanHaqiqiDerajat),
            catatan = "Argumen sinus: ${formatAngka(data.argumenSinus)}."
        )

    // 14. Parallax Bulan ghurub
    private fun tampilkanParallaxBulanGhurub(data: ParallaxBulanGhurub): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 14,
            judul = "Parallax Bulan pada jam Ghurub",
            rumus = "Parallax = Horizontal Parallax x cos h bulan.",
            substitusi = "${formatDerajat(data.horizontalParallax.hasilDerajat)} x cos ${formatDerajat(data.tinggiBulanHaqiqiDerajat)}",
            hasil = formatDerajat(data.parallaxDerajat),
            sumber = listOf(data.horizontalParallax.nilaiAtas.sumber, data.horizontalParallax.nilaiBawah.sumber)
        )

    // 15. Semi diameter Bulan ghurub
    private fun tampilkanSemiDiameterBulanGhurub(data: SemiDiameterBulanGhurub): ButirPerhitunganFalak =
        tampilkanInterpolasi(
            nomor = 15,
            judul = "Semi Diameter Bulan pada jam Ghurub",
            rumus = "Sd bulan ghurub = Na - (Na - Nb) x Nc.",
            interpolasi = data.interpolasi
        )

    // 16. ho Bulan ghurub
    private fun tampilkanHoBulanGhurub(data: HoBulanGhurub): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 16,
            judul = "ho Bulan pada jam Ghurub",
            rumus = "ho = h bulan - parallax + Sd bulan.",
            substitusi = "${formatDerajat(data.tinggiBulanHaqiqiDerajat)} - ${formatDerajat(data.parallaxDerajat)} + ${formatDerajat(data.semiDiameterBulanDerajat)}",
            hasil = formatDerajat(data.hoDerajat),
        )

    // 17. Refraksi hilal
    private fun tampilkanRefraksiHilal(data: RefraksiHilal): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 17,
            judul = "Refraksi Hilal",
            rumus = if (data.menggunakanRefraksiRataRata) {
                "Jika ho <= 0°, refraksi memakai nilai rata-rata 0°34'30\"."
            } else {
                "Refraksi = 0,0167 / tan(ho + 7,31 / (ho + 4,4))."
            },
            substitusi = if (data.menggunakanRefraksiRataRata) {
                "ho=${formatDerajat(data.hoDerajat)} <= 0°"
            } else {
                "0,0167 / tan(${formatDerajat(data.hoDerajat)} + 7,31 / (${formatDerajat(data.hoDerajat)} + 4,4))"
            },
            hasil = formatDerajat(data.refraksiDerajat),
        )

    // 18. Tinggi hilal mar'i tepi atas
    private fun tampilkanTinggiBulanMariGhurub(data: TinggiBulanMariGhurub): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 18,
            judul = "Tinggi Hilal Mar'i Tepi Atas pada jam Ghurub",
            rumus = "h' tepi atas = ho + refraksi + dip.",
            substitusi = "${formatDerajat(data.hoDerajat)} + ${formatDerajat(data.refraksiDerajat)} + ${formatDerajat(data.dipDerajat)}",
            hasil = formatDerajat(data.tinggiBulanMariDerajat),
        )

    // 19. Nishful fadhlah Bulan
    private fun tampilkanNishfulFadhlahBulan(data: NishfulFadhlahBulan): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 19,
            judul = "Nishful Fadhlah Bulan",
            rumus = "NF = sin^-1((sin phi x sin d bulan) : (cos phi x cos d bulan)).",
            substitusi = "sin^-1((sin ${formatDerajat(data.lintangMarkazDerajat)} x sin ${formatDerajat(data.deklinasiBulanDerajat)}) : (cos ${formatDerajat(data.lintangMarkazDerajat)} x cos ${formatDerajat(data.deklinasiBulanDerajat)}))",
            hasil = formatDerajat(data.nfDerajat),
            catatan = "Argumen sinus: ${formatAngka(data.argumenSinus)}."
        )

    // 20. Parallax nishful fadhlah
    private fun tampilkanParallaxNishfulFadhlah(data: ParallaxNishfulFadhlah): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 20,
            judul = "Parallax Nishful Fadhlah",
            rumus = "PNF = cos NF x Horizontal Parallax.",
            substitusi = "cos ${formatDerajat(data.nfDerajat)} x ${formatDerajat(data.horizontalParallaxDerajat)}",
            hasil = formatDerajat(data.pnfDerajat),
        )

    // 21. Setengah busur siang Bulan haqiqi
    private fun tampilkanSetengahBusurSiangBulanHaqiqi(data: SetengahBusurSiangBulanHaqiqi): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 21,
            judul = "Setengah Busur Siang Bulan Haqiqi",
            rumus = "SBSH = 90 + NF.",
            substitusi = "90° + ${formatDerajat(data.nfDerajat)}",
            hasil = formatDerajat(data.sbshDerajat),
        )

    // 22. Setengah busur siang Bulan
    private fun tampilkanSetengahBusurSiangBulan(data: SetengahBusurSiangBulan): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 22,
            judul = "Setengah Busur Siang Bulan",
            rumus = if (data.menggunakanRumusSbshLebihDari90) {
                "Karena SBSH > 90°, SBS = 90 + NF - PNF + (SD + 0°34'30\" + Dip)."
            } else {
                "Karena SBSH < 90°, SBS = 90 + NF + PNF - (SD + 0°34'30\" + Dip)."
            },
            substitusi = if (data.menggunakanRumusSbshLebihDari90) {
                "90° + ${formatDerajat(data.nfDerajat)} - ${formatDerajat(data.pnfDerajat)} + (${formatDerajat(data.semiDiameterBulanDerajat)} + ${formatDerajat(data.refraksiRataRataDerajat)} + ${formatDerajat(data.dipDerajat)})"
            } else {
                "90° + ${formatDerajat(data.nfDerajat)} + ${formatDerajat(data.pnfDerajat)} - (${formatDerajat(data.semiDiameterBulanDerajat)} + ${formatDerajat(data.refraksiRataRataDerajat)} + ${formatDerajat(data.dipDerajat)})"
            },
            hasil = formatDerajat(data.sbsDerajat),
            catatan = "SBSH=${formatDerajat(data.sbshDerajat)}."
        )

    // 23. Lama hilal / mukuts
    private fun tampilkanLamaHilalMukuts(data: LamaHilalMukuts): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 23,
            judul = "Lama Hilal / Mukuts",
            rumus = "Lama hilal = (SBS - t bulan) : 15.",
            substitusi = "(${formatDerajat(data.sbsDerajat)} - ${formatDerajat(data.sudutWaktuBulanDerajat)}) : 15",
            hasil = "${formatAngka(data.lamaHilalJam)} jam",
        )

    // 24. Terbenam hilal
    private fun tampilkanTerbenamHilal(data: TerbenamHilal): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 24,
            judul = "Terbenam Hilal",
            rumus = "Terbenam hilal = ghurub + lama hilal.",
            substitusi = "${formatJam(data.ghurub.jamDesimal)} ${data.ghurub.zona} + ${formatAngka(data.lamaHilalJam)} jam",
            hasil = "${formatJam(data.waktuLokal.jamDesimal)} ${data.waktuLokal.zona}, ${data.waktuLokal.tanggal}",
        )

    // 25. Azimut Matahari ghurub
    private fun tampilkanAzimutMatahariGhurub(data: AzimutMatahariGhurub): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 25,
            judul = "Azimut Matahari pada jam Ghurub",
            rumus = "Az matahari = tan^-1(-sin phi : tan t matahari + cos phi x tan d matahari : sin t matahari).",
            substitusi = "tan^-1(-sin ${formatDerajat(data.lintangMarkazDerajat)} : tan ${formatDerajat(data.sudutWaktuMatahariDerajat)} + cos ${formatDerajat(data.lintangMarkazDerajat)} x tan ${formatDerajat(data.deklinasiMatahariDerajat)} : sin ${formatDerajat(data.sudutWaktuMatahariDerajat)})",
            hasil = "${formatDerajat(data.azimutDerajat)} dari titik Barat ke ${data.arahDariBarat}",
            catatan = "Argumen tangen: ${formatAngka(data.argumenTangen)}."
        )

    // 26. Azimut Bulan ghurub
    private fun tampilkanAzimutBulanGhurub(data: AzimutBulanGhurub): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 26,
            judul = "Azimut Bulan pada jam Ghurub",
            rumus = "Az bulan = tan^-1(-sin phi : tan t bulan + cos phi x tan d bulan : sin t bulan).",
            substitusi = "tan^-1(-sin ${formatDerajat(data.lintangMarkazDerajat)} : tan ${formatDerajat(data.sudutWaktuBulanDerajat)} + cos ${formatDerajat(data.lintangMarkazDerajat)} x tan ${formatDerajat(data.deklinasiBulanDerajat)} : sin ${formatDerajat(data.sudutWaktuBulanDerajat)})",
            hasil = "${formatDerajat(data.azimutDerajat)} dari titik Barat ke ${data.arahDariBarat}",
            catatan = "Argumen tangen: ${formatAngka(data.argumenTangen)}."
        )

    // 27. Posisi hilal
    private fun tampilkanPosisiHilal(data: PosisiHilal): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 27,
            judul = "Posisi Hilal",
            rumus = "PH = Azimut Bulan - Azimut Matahari.",
            substitusi = "${formatDerajat(data.azimutBulanDerajat)} - ${formatDerajat(data.azimutMatahariDerajat)}",
            hasil = "${formatDerajat(data.posisiHilalDerajat)}; hilal di ${data.arahDariMatahari}",
        )

    // 28. Arah terbenam hilal
    private fun tampilkanArahTerbenamHilal(data: ArahTerbenamHilal): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 28,
            judul = "Arah Terbenam Hilal",
            rumus = "AT = tan^-1(-sin phi : tan SBS + cos phi x tan d bulan : sin SBS).",
            substitusi = "tan^-1(-sin ${formatDerajat(data.lintangMarkazDerajat)} : tan ${formatDerajat(data.sbsDerajat)} + cos ${formatDerajat(data.lintangMarkazDerajat)} x tan ${formatDerajat(data.deklinasiBulanDerajat)} : sin ${formatDerajat(data.sbsDerajat)})",
            hasil = formatDerajat(data.arahTerbenamDerajat),
            catatan = "Argumen tangen: ${formatAngka(data.argumenTangen)}."
        )

    // 29. Luas cahaya hilal / FIB ghurub
    private fun tampilkanLuasCahayaHilal(data: LuasCahayaHilal): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 29,
            judul = "Illuminasi Bulan / FIB pada jam Ghurub",
            rumus = "FIB ghurub = Na - (Na - Nb) x Nc.",
            substitusi = "${formatAngka(data.fibGhurub.nilaiAtas.nilai)} - (${formatAngka(data.fibGhurub.nilaiAtas.nilai)} - ${formatAngka(data.fibGhurub.nilaiBawah.nilai)}) x ${formatAngka(data.fibGhurub.nc)}",
            hasil = "${formatAngka(data.fibGhurub.hasil)} ${data.fibGhurub.satuan}",
            catatan = "Nc adalah pecahan jam ghurub GMT/UT dari jam ${data.fibGhurub.jamAtasUt} menuju jam ${data.fibGhurub.jamBawahUt}.",
            sumber = listOf(data.fibGhurub.nilaiAtas.sumber, data.fibGhurub.nilaiBawah.sumber)
        )

    // 30. Lebar nurul hilal
    private fun tampilkanLebarNurulHilal(data: LebarNurulHilal): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 30,
            judul = "Lebar Nurul Hilal",
            rumus = "NH = sqrt(PH^2 + h' bulan^2) : 15.",
            substitusi = "sqrt(${formatDerajat(kotlin.math.abs(data.posisiHilalDerajat))}^2 + ${formatDerajat(data.tinggiBulanMariDerajat)}^2) : 15",
            hasil = "${formatAngka(data.nurulHilalJari)} jari",
            catatan = "Tanda minus pada PH dihilangkan sesuai dokumen."
        )

    // 31. Kemiringan hilal
    private fun tampilkanKemiringanHilal(data: KemiringanHilal): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 31,
            judul = "Kemiringan Hilal",
            rumus = "MH = tan^-1(PH : h' bulan).",
            substitusi = "tan^-1(${formatDerajat(data.posisiHilalDerajat)} : ${formatDerajat(data.tinggiBulanMariDerajat)})",
            hasil = "${formatDerajat(data.kemiringanDerajat)}; ${data.keadaan}",
        )

    // 32. Jarak busur / elongasi
    private fun tampilkanJarakBusurElongasi(data: JarakBusurElongasi): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = 32,
            judul = "Jarak Busur / Elongasi Geosentrik",
            rumus = "JB = cos^-1(sin d matahari x sin d bulan + cos d matahari x cos d bulan x cos(AR matahari - AR bulan)).",
            substitusi = "cos^-1(sin ${formatDerajat(data.deklinasiMatahariDerajat)} x sin ${formatDerajat(data.deklinasiBulanDerajat)} + cos ${formatDerajat(data.deklinasiMatahariDerajat)} x cos ${formatDerajat(data.deklinasiBulanDerajat)} x cos(${formatDerajat(data.asensiorektaMatahariDerajat)} - ${formatDerajat(data.asensiorektaBulanDerajat)}))",
            hasil = formatDerajat(data.elongasiDerajat),
            catatan = "Argumen cosinus: ${formatAngka(data.argumenCosinus)}."
        )

    private fun tampilkanInterpolasi(
        nomor: Int,
        judul: String,
        rumus: String,
        interpolasi: InterpolasiEphemerisFalak,
    ): ButirPerhitunganFalak =
        ButirPerhitunganFalak(
            nomor = nomor,
            judul = judul,
            rumus = rumus,
            substitusi = substitusiInterpolasi(interpolasi),
            hasil = formatDerajat(interpolasi.hasilDerajat),
            catatan = "Nc adalah pecahan jam ghurub GMT/UT dari jam ${interpolasi.jamAtasUt} menuju jam ${interpolasi.jamBawahUt}.",
            sumber = listOf(interpolasi.nilaiAtas.sumber, interpolasi.nilaiBawah.sumber)
        )

    private fun barisEphemeris(
        ephemerisPerTanggal: Map<LocalDate, FalakEphemerisHarian>,
        tanggalAwal: LocalDate,
        jamUt: Int,
        tabel: TabelEphemeris,
    ): BarisEphemeris {
        if (jamUt == 24) {
            val dataTanggalAwal = ephemerisPerTanggal[tanggalAwal]
            val rowsTanggalAwal = when (tabel) {
                TabelEphemeris.MATAHARI -> dataTanggalAwal?.hourlyTable?.sun
                TabelEphemeris.BULAN -> dataTanggalAwal?.hourlyTable?.moon
            }
            val row24 = rowsTanggalAwal?.firstOrNull { it.hourUt() == 24 }
            if (row24 != null) return BarisEphemeris(tanggalAwal, 24, row24)
        }
        val tanggal = tanggalAwal.plusDays(Math.floorDiv(jamUt, 24).toLong())
        val jam = Math.floorMod(jamUt, 24)
        val dataTanggal = ephemerisPerTanggal[tanggal] ?: error("Data ephemeris tanggal $tanggal tidak tersedia.")
        val rows = if (tabel == TabelEphemeris.MATAHARI) dataTanggal.hourlyTable.sun else dataTanggal.hourlyTable.moon
        val row = rows.firstOrNull { it.hourUt() == jam }
            ?: rows.firstOrNull { it.hourUt() == jamUt }
            ?: error("Data ${tabel.label} jam $jam GMT/UT tanggal $tanggal tidak tersedia.")
        return BarisEphemeris(tanggal, row.hourUt(), row)
    }

    private fun interpolasiGhurub(
        konteks: KonteksHisabHilal,
        ephemerisPerTanggal: Map<LocalDate, FalakEphemerisHarian>,
        saatGhurub: SaatGhurub,
        tabel: TabelEphemeris,
        kolom: String,
        mode: ModeInterpolasi,
    ): InterpolasiEphemerisFalak {
        val jamGhurubUt = saatGhurub.waktuLokal.jamDesimal - konteks.markaz.zonaWaktu.offsetJam
        val waktuGhurubUt = normalisasiWaktu(saatGhurub.waktuLokal.tanggal, jamGhurubUt, "GMT/UT")
        val jamAtasUt = floor(waktuGhurubUt.jamDesimal).toInt()
        val jamBawahUt = jamAtasUt + 1
        val nc = waktuGhurubUt.jamDesimal - jamAtasUt
        val barisAtas = barisEphemeris(ephemerisPerTanggal, waktuGhurubUt.tanggal, jamAtasUt, tabel)
        val barisBawah = barisEphemeris(ephemerisPerTanggal, waktuGhurubUt.tanggal, jamBawahUt, tabel)
        val nilaiAtas = nilaiDerajat(barisAtas.tanggal, barisAtas.jamUt, tabel, kolom, barisAtas.row)
        val nilaiBawah = nilaiDerajat(barisBawah.tanggal, barisBawah.jamUt, tabel, kolom, barisBawah.row)
        val hasil = when (mode) {
            ModeInterpolasi.LINEAR -> nilaiAtas.nilai - (nilaiAtas.nilai - nilaiBawah.nilai) * nc
            ModeInterpolasi.SUDUT_MAJU -> normalisasiDerajat(nilaiAtas.nilai + deltaMajuDerajat(nilaiAtas.nilai, nilaiBawah.nilai) * nc)
        }
        return InterpolasiEphemerisFalak(
            jamAtasUt = barisAtas.jamUt,
            jamBawahUt = barisBawah.jamUt,
            nc = nc,
            nilaiAtas = nilaiAtas,
            nilaiBawah = nilaiBawah,
            hasilDerajat = hasil,
        )
    }

    private fun interpolasiAngkaGhurub(
        konteks: KonteksHisabHilal,
        ephemerisPerTanggal: Map<LocalDate, FalakEphemerisHarian>,
        saatGhurub: SaatGhurub,
        tabel: TabelEphemeris,
        kolom: String,
        satuan: String,
    ): InterpolasiAngkaEphemerisFalak {
        val jamGhurubUt = saatGhurub.waktuLokal.jamDesimal - konteks.markaz.zonaWaktu.offsetJam
        val waktuGhurubUt = normalisasiWaktu(saatGhurub.waktuLokal.tanggal, jamGhurubUt, "GMT/UT")
        val jamAtasUt = floor(waktuGhurubUt.jamDesimal).toInt()
        val jamBawahUt = jamAtasUt + 1
        val nc = waktuGhurubUt.jamDesimal - jamAtasUt
        val barisAtas = barisEphemeris(ephemerisPerTanggal, waktuGhurubUt.tanggal, jamAtasUt, tabel)
        val barisBawah = barisEphemeris(ephemerisPerTanggal, waktuGhurubUt.tanggal, jamBawahUt, tabel)
        val nilaiAtas = nilaiAngka(barisAtas.tanggal, barisAtas.jamUt, tabel, kolom, barisAtas.row)
        val nilaiBawah = nilaiAngka(barisBawah.tanggal, barisBawah.jamUt, tabel, kolom, barisBawah.row)
        return InterpolasiAngkaEphemerisFalak(
            jamAtasUt = barisAtas.jamUt,
            jamBawahUt = barisBawah.jamUt,
            nc = nc,
            nilaiAtas = nilaiAtas,
            nilaiBawah = nilaiBawah,
            hasil = nilaiAtas.nilai - (nilaiAtas.nilai - nilaiBawah.nilai) * nc,
            satuan = satuan,
        )
    }

    private fun nilaiDerajat(
        tanggal: LocalDate,
        jamUt: Int,
        tabel: TabelEphemeris,
        kolom: String,
        row: JsonObject,
    ): NilaiEphemerisFalak {
        val obj = row[kolom]?.jsonObjectOrNull() ?: error("Kolom $kolom tidak tersedia pada ${tabel.label} jam $jamUt GMT/UT.")
        val value = obj.doubleAt("decimal_degree") ?: error("Nilai decimal_degree kolom $kolom tidak tersedia.")
        return NilaiEphemerisFalak(
            nilai = value,
            raw = obj.textAt("raw"),
            sumber = SumberEphemerisFalak(tanggal, jamUt, tabel.label, kolom, obj.textAt("raw"))
        )
    }

    private fun nilaiAngka(
        tanggal: LocalDate,
        jamUt: Int,
        tabel: TabelEphemeris,
        kolom: String,
        row: JsonObject,
    ): NilaiEphemerisFalak {
        val value = row.doubleAt(kolom) ?: error("Nilai $kolom tidak tersedia pada ${tabel.label} jam $jamUt GMT/UT.")
        return NilaiEphemerisFalak(
            nilai = value,
            raw = row.textAt(kolom),
            sumber = SumberEphemerisFalak(tanggal, jamUt, tabel.label, kolom, row.textAt(kolom))
        )
    }

    private fun nilaiJam(
        tanggal: LocalDate,
        jamUt: Int,
        tabel: TabelEphemeris,
        kolom: String,
        row: JsonObject,
    ): NilaiEphemerisFalak {
        val obj = row[kolom]?.jsonObjectOrNull() ?: error("Kolom $kolom tidak tersedia pada ${tabel.label} jam $jamUt GMT/UT.")
        val value = obj.doubleAt("hours") ?: error("Nilai hours kolom $kolom tidak tersedia.")
        return NilaiEphemerisFalak(
            nilai = value,
            raw = obj.textAt("raw"),
            sumber = SumberEphemerisFalak(tanggal, jamUt, tabel.label, kolom, obj.textAt("raw"))
        )
    }

    private fun normalisasiWaktu(tanggalAwal: LocalDate, jam: Double, zona: String): WaktuFalak {
        val days = floor(jam / 24.0).toLong()
        var normalizedHour = jam - (days * 24.0)
        var date = tanggalAwal.plusDays(days)
        if (normalizedHour < 0.0) {
            normalizedHour += 24.0
            date = date.minusDays(1)
        }
        return WaktuFalak(date, normalizedHour, zona)
    }

    private fun JsonObject.hourUt(): Int =
        this["hour_ut"]?.jsonPrimitive?.intOrNull ?: error("hour_ut tidak tersedia.")

    private fun JsonObject.doubleAt(name: String): Double? =
        this[name]?.doubleContentOrNull()

    private fun JsonObject.textAt(name: String): String? =
        this[name]?.jsonPrimitiveOrNull()?.contentOrNull

    private fun JsonElement.doubleContentOrNull(): Double? =
        runCatching { jsonPrimitive.doubleOrNull ?: jsonPrimitive.content.replace(",", ".").toDoubleOrNull() }.getOrNull()

    private fun JsonElement.jsonObjectOrNull(): JsonObject? =
        runCatching { jsonObject }.getOrNull()

    private fun JsonElement.jsonPrimitiveOrNull() =
        runCatching { jsonPrimitive }.getOrNull()

    private fun deltaMajuDerajat(awal: Double, setelah: Double): Double {
        var delta = setelah - awal
        while (delta < 0.0) delta += 360.0
        return delta
    }

    private fun selisihSudutBertanda(nilaiKiri: Double, nilaiKanan: Double): Double {
        var delta = nilaiKiri - nilaiKanan
        while (delta > 180.0) delta -= 360.0
        while (delta <= -180.0) delta += 360.0
        return delta
    }

    private fun normalisasiDerajat(value: Double): Double {
        var normalized = value % 360.0
        if (normalized < 0.0) normalized += 360.0
        return normalized
    }

    private fun sinDeg(value: Double): Double = sin(value * PI / 180.0)

    private fun cosDeg(value: Double): Double = cos(value * PI / 180.0)

    private fun tanDeg(value: Double): Double = tan(value * PI / 180.0)

    private fun arahAzimutDariBarat(azimutDerajat: Double): String =
        if (azimutDerajat >= 0.0) "utara" else "selatan"

    private fun formatDerajat(value: Double): String {
        val sign = if (value < 0.0) "-" else ""
        val totalCentiseconds = (kotlin.math.abs(value) * 3600.0 * 100.0).roundToLong()
        val degree = totalCentiseconds / 360000
        val minute = (totalCentiseconds % 360000) / 6000
        val second = (totalCentiseconds % 6000) / 100.0
        return "$sign${degree}° %02d' %05.2f\"".format(Locale.US, minute, second)
    }

    private fun substitusiInterpolasi(interpolasi: InterpolasiEphemerisFalak): String =
        "${formatDerajat(interpolasi.nilaiAtas.nilai)} - (${formatDerajat(interpolasi.nilaiAtas.nilai)} - ${formatDerajat(interpolasi.nilaiBawah.nilai)}) x ${formatAngka(interpolasi.nc)}"

    private fun formatAngka(value: Double): String = "%.6f".format(Locale.US, value).trimEnd('0').trimEnd('.')

    private fun formatJam(value: Double): String {
        val totalSeconds = (value * 3600.0).toLong()
        val hour = totalSeconds / 3600
        val minute = (totalSeconds % 3600) / 60
        val second = totalSeconds % 60
        return "%02d:%02d:%02d".format(Locale.US, hour, minute, second)
    }

    private fun formatTanggalLengkap(tanggal: LocalDate): String =
        tanggal.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID")))

    private data class BarisEphemeris(
        val tanggal: LocalDate,
        val jamUt: Int,
        val row: JsonObject,
    )

    private enum class TabelEphemeris(val label: String) {
        MATAHARI("Data Matahari"),
        BULAN("Data Bulan"),
    }

    private enum class ModeInterpolasi {
        LINEAR,
        SUDUT_MAJU,
    }
}
