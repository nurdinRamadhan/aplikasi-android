package com.alhasanah.alhasanahmedia.domain.falak

import com.alhasanah.alhasanahmedia.data.model.falak.FalakEphemerisHarian
import com.alhasanah.alhasanahmedia.data.model.falak.FalakHourlyTable
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HisabHilalEphemerisCalculatorTest {

    @Test
    fun menghitungDataIjtimaSabaqIjtimaSaatIjtimaDanPosisiMatahariHaqiqiGhurub() {
        val konteks = KonteksHisabHilal(
            bulanHijriah = "Ramadan 1447 H",
            tanggalSituasiHilalMasehi = LocalDate.parse("2026-01-01"),
            markaz = MarkazFalak(
                nama = "Markaz Uji",
                lintangDerajat = -6.2,
                bujurDerajat = 106.8,
                elevasiMeter = 100.0,
                zonaWaktu = ZonaWaktuFalak.WIB,
            )
        )
        val ephemeris = listOf(
            FalakEphemerisHarian(
                date = "2026-01-01",
                hasStructuredHourlyTable = true,
                hourlyTable = FalakHourlyTable(
                    sun = listOf(
                        sunRow(5, 99.80, apparentRightAscension = 95.0, semiDiameter = 0.270000),
                        sunRow(6, 99.85, apparentRightAscension = 96.0, semiDiameter = 0.270000),
                        sunRow(11, 100.10, apparentRightAscension = 101.0, semiDiameter = 0.271000),
                        sunRow(12, 100.15, apparentRightAscension = 102.0, semiDiameter = 0.271000),
                    ),
                    moon = listOf(
                        moonRow(5, fib = 0.20, apparentLongitude = 100.00, apparentRightAscension = 90.0, apparentDeclination = -9.0),
                        moonRow(6, fib = 0.30, apparentLongitude = 100.60, apparentRightAscension = 91.0, apparentDeclination = -9.2),
                        moonRow(11, fib = 4.00, apparentLongitude = 103.00, apparentRightAscension = 99.0, apparentDeclination = -10.0),
                        moonRow(12, fib = 4.50, apparentLongitude = 103.60, apparentRightAscension = 100.5, apparentDeclination = -11.0),
                    )
                )
            )
        )

        val result = HisabHilalEphemerisCalculator().hitung(konteks, ephemeris)

        assertEquals(32, result.butirPerhitungan.size)
        assertEquals(5, result.dataIjtima.jamFibUt)
        assertEquals(0.60, result.sabaqIjtima.sabaqBulanDerajat, 0.000001)
        assertEquals(0.05, result.sabaqIjtima.sabaqMatahariDerajat, 0.000001)
        assertEquals(4.636363, result.saatIjtima.waktuUt.jamDesimal, 0.000001)
        assertEquals(11.636363, result.saatIjtima.waktuLokal.jamDesimal, 0.000001)

        val expectedDip = sqrt(100.0) * 0.0293
        val expectedSunAltitude = 0.0 - 0.271 - (34.5 / 60.0) - expectedDip
        assertEquals(expectedDip, result.posisiMatahariHaqiqiGhurub.dipDerajat, 0.000001)
        assertEquals(expectedSunAltitude, result.posisiMatahariHaqiqiGhurub.tinggiMatahariHaqiqiDerajat, 0.000001)
        assertTrue(result.butirPerhitungan[4].judul.contains("Posisi Matahari Haqiqi"))

        val expectedCosArgument = -tanDeg(-6.2) * tanDeg(-20.0) + sinDeg(expectedSunAltitude) / cosDeg(-6.2) / cosDeg(-20.0)
        val expectedHourAngle = acos(expectedCosArgument) * 180.0 / PI
        val expectedKwd = (105.0 - 106.8) / 15.0
        val expectedGhurub = (expectedHourAngle / 15.0) + (12.0 - (-0.05)) + expectedKwd
        assertEquals(expectedCosArgument, result.sudutWaktuMatahariGhurub.argumenCosinus, 0.000001)
        assertEquals(expectedHourAngle, result.sudutWaktuMatahariGhurub.sudutWaktuDerajat, 0.000001)
        assertEquals(expectedKwd, result.koreksiWaktuDaerah.koreksiJam, 0.000001)
        assertEquals(expectedGhurub, result.saatGhurub.waktuLokal.jamDesimal, 0.000001)
        assertTrue(result.butirPerhitungan[7].judul.contains("Ghurub"))

        val ghurubUt = expectedGhurub - 7.0
        val nc = ghurubUt - ghurubUt.toInt()
        assertEquals(101.0 + (102.0 - 101.0) * nc, result.asensiorektaMatahariGhurub.interpolasi.hasilDerajat, 0.000001)
        assertEquals(99.0 + (100.5 - 99.0) * nc, result.asensiorektaBulanGhurub.interpolasi.hasilDerajat, 0.000001)
        assertEquals(
            (result.asensiorektaMatahariGhurub.interpolasi.hasilDerajat - result.asensiorektaBulanGhurub.interpolasi.hasilDerajat) + expectedHourAngle,
            result.sudutWaktuBulanGhurub.sudutWaktuBulanDerajat,
            0.000001
        )
        assertEquals(-20.0, result.deklinasiGhurub.matahari.hasilDerajat, 0.000001)
        assertEquals(-10.0 + (-11.0 - -10.0) * nc, result.deklinasiGhurub.bulan.hasilDerajat, 0.000001)
        val expectedMoonAltitudeArgument = sinDeg(-6.2) * sinDeg(result.deklinasiGhurub.bulan.hasilDerajat) +
            cosDeg(-6.2) * cosDeg(result.deklinasiGhurub.bulan.hasilDerajat) * cosDeg(result.sudutWaktuBulanGhurub.sudutWaktuBulanDerajat)
        val expectedMoonAltitude = kotlin.math.asin(expectedMoonAltitudeArgument) * 180.0 / PI
        val expectedHorizontalParallax = 0.95 + (0.94 - 0.95) * nc
        val expectedMoonSemiDiameter = 0.255 + (0.254 - 0.255) * nc
        val expectedParallax = expectedHorizontalParallax * cosDeg(expectedMoonAltitude)
        val expectedHo = expectedMoonAltitude - expectedParallax + expectedMoonSemiDiameter
        val expectedRefraction = if (expectedHo <= 0.0) 34.5 / 60.0 else 0.0167 / tanDeg(expectedHo + 7.31 / (expectedHo + 4.4))
        assertEquals(expectedMoonAltitude, result.tinggiBulanHaqiqiGhurub.tinggiBulanHaqiqiDerajat, 0.000001)
        assertEquals(expectedParallax, result.parallaxBulanGhurub.parallaxDerajat, 0.000001)
        assertEquals(expectedMoonSemiDiameter, result.semiDiameterBulanGhurub.interpolasi.hasilDerajat, 0.000001)
        assertEquals(expectedHo, result.hoBulanGhurub.hoDerajat, 0.000001)
        assertEquals(expectedRefraction, result.refraksiHilal.refraksiDerajat, 0.000001)
        assertEquals(expectedHo + expectedRefraction + expectedDip, result.tinggiBulanMariGhurub.tinggiBulanMariDerajat, 0.000001)

        val expectedNfArgument = (sinDeg(-6.2) * sinDeg(result.deklinasiGhurub.bulan.hasilDerajat)) /
            (cosDeg(-6.2) * cosDeg(result.deklinasiGhurub.bulan.hasilDerajat))
        val expectedNf = kotlin.math.asin(expectedNfArgument) * 180.0 / PI
        val expectedPnf = cosDeg(expectedNf) * expectedHorizontalParallax
        val expectedSbsh = 90.0 + expectedNf
        val expectedSbs = if (expectedSbsh > 90.0) {
            90.0 + expectedNf - expectedPnf + (expectedMoonSemiDiameter + (34.5 / 60.0) + expectedDip)
        } else {
            90.0 + expectedNf + expectedPnf - (expectedMoonSemiDiameter + (34.5 / 60.0) + expectedDip)
        }
        val expectedMukuts = (expectedSbs - result.sudutWaktuBulanGhurub.sudutWaktuBulanDerajat) / 15.0
        assertEquals(expectedNf, result.nishfulFadhlahBulan.nfDerajat, 0.000001)
        assertEquals(expectedPnf, result.parallaxNishfulFadhlah.pnfDerajat, 0.000001)
        assertEquals(expectedSbsh, result.setengahBusurSiangBulanHaqiqi.sbshDerajat, 0.000001)
        assertEquals(expectedSbs, result.setengahBusurSiangBulan.sbsDerajat, 0.000001)
        assertEquals(expectedMukuts, result.lamaHilalMukuts.lamaHilalJam, 0.000001)
        assertEquals(expectedGhurub + expectedMukuts, result.terbenamHilal.waktuLokal.jamDesimal, 0.000001)

        val expectedAzSunArgument = (-sinDeg(-6.2) / tanDeg(expectedHourAngle)) +
            (cosDeg(-6.2) * tanDeg(-20.0) / sinDeg(expectedHourAngle))
        val expectedAzSun = kotlin.math.atan(expectedAzSunArgument) * 180.0 / PI
        val expectedAzMoonArgument = (-sinDeg(-6.2) / tanDeg(result.sudutWaktuBulanGhurub.sudutWaktuBulanDerajat)) +
            (cosDeg(-6.2) * tanDeg(result.deklinasiGhurub.bulan.hasilDerajat) / sinDeg(result.sudutWaktuBulanGhurub.sudutWaktuBulanDerajat))
        val expectedAzMoon = kotlin.math.atan(expectedAzMoonArgument) * 180.0 / PI
        val expectedPosisiHilal = expectedAzMoon - expectedAzSun
        val expectedAtArgument = (-sinDeg(-6.2) / tanDeg(expectedSbs)) +
            (cosDeg(-6.2) * tanDeg(result.deklinasiGhurub.bulan.hasilDerajat) / sinDeg(expectedSbs))
        val expectedAt = kotlin.math.atan(expectedAtArgument) * 180.0 / PI
        val expectedFibGhurub = 4.0 + (4.5 - 4.0) * nc
        val expectedNurulHilal = sqrt(expectedPosisiHilal * expectedPosisiHilal + result.tinggiBulanMariGhurub.tinggiBulanMariDerajat * result.tinggiBulanMariGhurub.tinggiBulanMariDerajat) / 15.0
        val expectedKemiringan = kotlin.math.abs(
            kotlin.math.atan(expectedPosisiHilal / result.tinggiBulanMariGhurub.tinggiBulanMariDerajat) * 180.0 / PI
        )
        val expectedElongationArgument = sinDeg(result.deklinasiGhurub.matahari.hasilDerajat) * sinDeg(result.deklinasiGhurub.bulan.hasilDerajat) +
            cosDeg(result.deklinasiGhurub.matahari.hasilDerajat) * cosDeg(result.deklinasiGhurub.bulan.hasilDerajat) *
            cosDeg(result.asensiorektaMatahariGhurub.interpolasi.hasilDerajat - result.asensiorektaBulanGhurub.interpolasi.hasilDerajat)
        val expectedElongation = acos(expectedElongationArgument) * 180.0 / PI
        assertEquals(expectedAzSun, result.azimutMatahariGhurub.azimutDerajat, 0.000001)
        assertEquals(expectedAzMoon, result.azimutBulanGhurub.azimutDerajat, 0.000001)
        assertEquals(expectedPosisiHilal, result.posisiHilal.posisiHilalDerajat, 0.000001)
        assertEquals(expectedAt, result.arahTerbenamHilal.arahTerbenamDerajat, 0.000001)
        assertEquals(expectedFibGhurub, result.luasCahayaHilal.fibGhurub.hasil, 0.000001)
        assertEquals(expectedNurulHilal, result.lebarNurulHilal.nurulHilalJari, 0.000001)
        assertEquals(expectedKemiringan, result.kemiringanHilal.kemiringanDerajat, 0.000001)
        assertEquals(expectedElongation, result.jarakBusurElongasi.elongasiDerajat, 0.000001)
        assertEquals(KriteriaAwalBulanFalak.KemenagMabimsTerbaru, result.kesimpulan.kriteriaAwalBulan)
        assertEquals(result.saatIjtima.waktuLokal, result.kesimpulan.ijtima)
        assertEquals(result.saatGhurub.waktuLokal, result.kesimpulan.ghurub)
        assertEquals(expectedMoonSemiDiameter, result.semiDiameterBulanGhurub.interpolasi.hasilDerajat, 0.000001)
        assertEquals(result.tinggiBulanMariGhurub.tinggiBulanMariDerajat, result.kesimpulan.tinggiHilalMariDerajat, 0.000001)
        assertEquals(result.lamaHilalMukuts.lamaHilalJam, result.kesimpulan.lamaHilalJam, 0.000001)
        assertEquals(result.jarakBusurElongasi.elongasiDerajat, result.kesimpulan.elongasiHilalDerajat, 0.000001)
        assertEquals(false, result.kesimpulan.memenuhiKriteria)
        assertEquals(LocalDate.parse("2026-01-03"), result.kesimpulan.tanggalPrakiraanAwalBulanMasehi)
    }

    private fun sunRow(
        hour: Int,
        apparentEclipticLongitude: Double,
        apparentRightAscension: Double,
        semiDiameter: Double,
    ): JsonObject = buildJsonObject {
        put("hour_ut", hour)
        put("apparent_ecliptic_longitude", angle(apparentEclipticLongitude))
        put("apparent_right_ascension", angle(apparentRightAscension))
        put("apparent_declination", angle(-20.0))
        put("semi_diameter", angle(semiDiameter))
        put("equation_of_time", timeHours(-0.05))
    }

    private fun moonRow(
        hour: Int,
        fib: Double,
        apparentLongitude: Double,
        apparentRightAscension: Double,
        apparentDeclination: Double,
    ): JsonObject = buildJsonObject {
        put("hour_ut", hour)
        put("fraction_illumination_percent", fib)
        put("apparent_longitude", angle(apparentLongitude))
        put("apparent_right_ascension", angle(apparentRightAscension))
        put("apparent_declination", angle(apparentDeclination))
        put("horizontal_parallax", angle(if (hour == 12) 0.94 else 0.95))
        put("semi_diameter", angle(if (hour == 12) 0.254 else 0.255))
    }

    private fun angle(value: Double): JsonObject = buildJsonObject {
        put("raw", "$value")
        put("decimal_degree", value)
    }

    private fun timeHours(value: Double): JsonObject = buildJsonObject {
        put("raw", "$value")
        put("hours", value)
    }

    private fun sinDeg(value: Double): Double = sin(value * PI / 180.0)

    private fun cosDeg(value: Double): Double = cos(value * PI / 180.0)

    private fun tanDeg(value: Double): Double = tan(value * PI / 180.0)
}
