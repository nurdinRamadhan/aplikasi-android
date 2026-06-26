package com.alhasanah.alhasanahmedia.domain.falak

import com.alhasanah.alhasanahmedia.data.model.falak.FalakEphemerisHarian
import com.alhasanah.alhasanahmedia.data.model.falak.FalakHourlyTable
import java.time.LocalDate
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun contohKonteksGerhana(): KonteksGerhanaBulan =
    KonteksGerhanaBulan(
        bulanHijriah = "Pertengahan Ramadan 1447 H",
        tanggalKemungkinanGerhanaMasehi = LocalDate.parse("2026-03-04"),
        zonaWaktu = ZonaWaktuFalak.WIB,
    )

fun contohEphemerisGerhanaBulan(): List<FalakEphemerisHarian> =
    listOf(
        FalakEphemerisHarian(
            date = "2026-03-03",
            hasStructuredHourlyTable = true,
            hourlyTable = FalakHourlyTable(
                sun = listOf(
                    contohSunRow(11, dmsFixture(342, 52, 19.0), dmsFixture(0, 16, 8.04), 0.9913095),
                    contohSunRow(12, dmsFixture(342, 54, 49.0), dmsFixture(0, 16, 8.03), 0.9913198),
                ),
                moon = listOf(
                    contohMoonRow(
                        hour = 11,
                        fib = 1.0,
                        apparentLongitude = dmsFixture(162, 32, 58.0),
                        apparentLatitude = -dmsFixture(0, 19, 45.0),
                        horizontalParallax = dmsFixture(0, 57, 20.0),
                        semiDiameter = dmsFixture(0, 15, 37.16),
                    ),
                    contohMoonRow(
                        hour = 12,
                        fib = 0.999,
                        apparentLongitude = dmsFixture(163, 6, 12.0),
                        apparentLatitude = -dmsFixture(0, 22, 49.0),
                        horizontalParallax = dmsFixture(0, 57, 18.0),
                        semiDiameter = dmsFixture(0, 15, 36.76),
                    ),
                )
            )
        ),
        FalakEphemerisHarian(date = "2026-03-04", hasStructuredHourlyTable = true),
    )

private fun contohSunRow(
    hour: Int,
    apparentEclipticLongitude: Double,
    semiDiameter: Double,
    trueGeocentricDistance: Double,
): JsonObject = buildJsonObject {
    put("hour_ut", hour)
    put("apparent_ecliptic_longitude", angleFixture(apparentEclipticLongitude))
    put("semi_diameter", angleFixture(semiDiameter))
    put("true_geocentric_distance", trueGeocentricDistance)
}

private fun contohMoonRow(
    hour: Int,
    fib: Double,
    apparentLongitude: Double,
    apparentLatitude: Double,
    horizontalParallax: Double,
    semiDiameter: Double,
): JsonObject = buildJsonObject {
    put("hour_ut", hour)
    put("fraction_illumination_percent", fib)
    put("apparent_longitude", angleFixture(apparentLongitude))
    put("apparent_latitude", angleFixture(apparentLatitude))
    put("horizontal_parallax", angleFixture(horizontalParallax))
    put("semi_diameter", angleFixture(semiDiameter))
}

private fun angleFixture(value: Double): JsonObject = buildJsonObject {
    put("raw", "$value")
    put("decimal_degree", value)
}

private fun dmsFixture(degree: Int, minute: Int, second: Double): Double =
    degree + minute / 60.0 + second / 3600.0
