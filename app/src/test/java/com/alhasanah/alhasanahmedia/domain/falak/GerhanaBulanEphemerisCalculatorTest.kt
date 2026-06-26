package com.alhasanah.alhasanahmedia.domain.falak

import com.alhasanah.alhasanahmedia.data.model.falak.FalakEphemerisHarian
import com.alhasanah.alhasanahmedia.data.model.falak.FalakHourlyTable
import java.time.LocalDate
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GerhanaBulanEphemerisCalculatorTest {

    @Test
    fun menghitungGerhanaBulanTotalBerdasarkanContohKemenagRamadan1447() {
        val konteks = KonteksGerhanaBulan(
            bulanHijriah = "Pertengahan Ramadan 1447 H",
            tanggalKemungkinanGerhanaMasehi = LocalDate.parse("2026-03-04"),
            zonaWaktu = ZonaWaktuFalak.WIB,
        )
        val ephemeris = listOf(
            FalakEphemerisHarian(
                date = "2026-03-03",
                hasStructuredHourlyTable = true,
                hourlyTable = FalakHourlyTable(
                    sun = listOf(
                        sunRow(11, dms(342, 52, 19.0), dms(0, 16, 8.04), 0.9913095),
                        sunRow(12, dms(342, 54, 49.0), dms(0, 16, 8.03), 0.9913198),
                    ),
                    moon = listOf(
                        moonRow(
                            hour = 11,
                            fib = 1.0,
                            apparentLongitude = dms(162, 32, 58.0),
                            apparentLatitude = -dms(0, 19, 45.0),
                            horizontalParallax = dms(0, 57, 20.0),
                            semiDiameter = dms(0, 15, 37.16),
                        ),
                        moonRow(
                            hour = 12,
                            fib = 0.999,
                            apparentLongitude = dms(163, 6, 12.0),
                            apparentLatitude = -dms(0, 22, 49.0),
                            horizontalParallax = dms(0, 57, 18.0),
                            semiDiameter = dms(0, 15, 36.76),
                        ),
                    )
                )
            ),
            FalakEphemerisHarian(date = "2026-03-04", hasStructuredHourlyTable = true),
        )

        val result = GerhanaBulanEphemerisCalculator().hitung(konteks, ephemeris)

        assertEquals(33, result.butirPerhitungan.size)
        assertEquals(11, result.dataIstiqbal.jamFibUt)
        assertEquals(dms(0, 2, 30.0), result.sabaq.sabaqMatahariDerajat, 0.000001)
        assertEquals(dms(0, 33, 14.0), result.sabaq.sabaqBulanDerajat, 0.000001)
        assertEquals(dms(0, 30, 44.0), result.sabaq.sabaqBulanMatahariDerajat, 0.000001)
        assertEquals(dms(0, 19, 21.0), result.saatIstiqbal.mbDerajat, 0.000001)
        assertEquals(11.59925, result.saatIstiqbal.waktuUt.jamDesimal, 0.00001)

        assertEquals(dms(0, 15, 36.92), result.dataInterpolasi.semiDiameterBulan.hasilDerajat, 0.00002)
        assertEquals(dms(0, 57, 18.80), result.dataInterpolasi.horizontalParallaxBulan.hasilDerajat, 0.00002)
        assertEquals(-dms(0, 21, 35.26), result.dataInterpolasi.lintangBulan.hasilDerajat, 0.00002)
        assertEquals(dms(0, 16, 8.03), result.dataInterpolasi.semiDiameterMatahari.hasilDerajat, 0.00002)
        assertEquals(0.9913157, result.dataInterpolasi.jarakBumiMatahari.hasil, 0.0000001)

        assertEquals(dms(0, 0, 8.87), result.bayanganBumi.parallaxMatahariDerajat, 0.00002)
        assertEquals(dms(0, 42, 9.23), result.bayanganBumi.semiDiameterBayanganIntiDerajat, 0.00003)
        assertEquals(dms(0, 57, 46.15), result.bayanganBumi.xDerajat, 0.00003)
        assertEquals(dms(0, 26, 32.31), result.bayanganBumi.yDerajat, 0.00003)

        assertEquals(-dms(4, 7, 54.23), result.simpul.hDerajat, 0.00003)
        assertEquals(dms(4, 58, 52.20), result.simpul.uDerajat, 0.00003)
        assertEquals(dms(0, 21, 30.39), result.simpul.zDerajat, 0.00003)
        assertEquals(dms(0, 30, 50.95), result.simpul.kDerajatPerJam, 0.00003)
        assertEquals(JenisGerhanaBulan.Total, result.klasifikasi.jenis)

        assertEquals(dms(0, 53, 37.02), result.jarakKontak.cDerajat, 0.00004)
        assertEquals(1.738036, result.jarakKontak.t1Jam, 0.00002)
        assertEquals(dms(0, 15, 32.93), result.jarakKontak.eDerajat ?: 0.0, 0.00004)
        assertEquals(0.504029, result.jarakKontak.t2Jam ?: 0.0, 0.00002)

        assertEquals(24.51 / 3600.0, result.koreksiTengah.t0Jam, 0.00003)
        assertEquals(136.61 / 3600.0, result.koreksiTengah.deltaTJam, 0.00002)
        assertEquals(18.55449, result.waktuKontak.tengahGerhanaLokal.jamDesimal, 0.00003)
        assertEquals(16.81645, result.waktuKontak.mulaiGerhanaLokal?.jamDesimal ?: 0.0, 0.00005)
        assertEquals(18.05046, result.waktuKontak.mulaiTotalLokal?.jamDesimal ?: 0.0, 0.00005)
        assertEquals(19.05852, result.waktuKontak.selesaiTotalLokal?.jamDesimal ?: 0.0, 0.00005)
        assertEquals(20.29252, result.waktuKontak.selesaiGerhanaLokal?.jamDesimal ?: 0.0, 0.00005)
        assertTrue(result.kesimpulan.status.contains("Gerhana Bulan Total"))
    }

    private fun sunRow(
        hour: Int,
        apparentEclipticLongitude: Double,
        semiDiameter: Double,
        trueGeocentricDistance: Double,
    ): JsonObject = buildJsonObject {
        put("hour_ut", hour)
        put("apparent_ecliptic_longitude", angle(apparentEclipticLongitude))
        put("semi_diameter", angle(semiDiameter))
        put("true_geocentric_distance", trueGeocentricDistance)
    }

    private fun moonRow(
        hour: Int,
        fib: Double,
        apparentLongitude: Double,
        apparentLatitude: Double,
        horizontalParallax: Double,
        semiDiameter: Double,
    ): JsonObject = buildJsonObject {
        put("hour_ut", hour)
        put("fraction_illumination_percent", fib)
        put("apparent_longitude", angle(apparentLongitude))
        put("apparent_latitude", angle(apparentLatitude))
        put("horizontal_parallax", angle(horizontalParallax))
        put("semi_diameter", angle(semiDiameter))
    }

    private fun angle(value: Double): JsonObject = buildJsonObject {
        put("raw", "$value")
        put("decimal_degree", value)
    }

    private fun dms(degree: Int, minute: Int, second: Double): Double =
        degree + minute / 60.0 + second / 3600.0
}
