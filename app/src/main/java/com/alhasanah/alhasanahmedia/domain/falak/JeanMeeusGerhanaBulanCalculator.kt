package com.alhasanah.alhasanahmedia.domain.falak

import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.floor

class JeanMeeusGerhanaBulanCalculator {

    fun prakiraanPurnamaTerdekat(tanggalInput: LocalDate): PembandingMeeusGerhanaBulan {
        val jdInput = julianDayAwalTanggal(tanggalInput)
        val kDasar = floor((tahunDesimal(tanggalInput) - 2000.0) * 12.3685)
        val kandidat = (-2..2).map { offset ->
            val k = kDasar + offset + 0.5
            val jde = julianEphemerisPurnama(k)
            val argumenLintang = normalisasiDerajat(
                160.7108 + 390.67050284 * k - 0.0016118 * t(k) * t(k) -
                    0.00000227 * t(k) * t(k) * t(k) + 0.000000011 * t(k) * t(k) * t(k) * t(k)
            )
            KandidatPurnama(jde, argumenLintang, abs(jde - jdInput))
        }.minBy { it.selisihHari }

        val tanggalJam = tanggalDariJulianDay(kandidat.julianDay)
        val jarakSimpul = jarakKeSimpul(kandidat.argumenLintangDerajat)
        val memungkinkan = jarakSimpul <= 13.9
        return PembandingMeeusGerhanaBulan(
            tanggalPurnamaTerdekatUt = tanggalJam.first,
            jamPurnamaTerdekatUt = tanggalJam.second,
            selisihHariDariInput = kandidat.julianDay - jdInput,
            argumenLintangBulanDerajat = kandidat.argumenLintangDerajat,
            jarakKeSimpulDerajat = jarakSimpul,
            memungkinkanGerhana = memungkinkan,
            catatan = if (memungkinkan) {
                "Pembanding Meeus menunjukkan purnama dekat simpul Bulan; lanjutkan audit dengan ephemeris."
            } else {
                "Pembanding Meeus menunjukkan purnama jauh dari simpul Bulan; gerhana biasanya tidak terjadi."
            },
        )
    }

    private fun t(k: Double): Double = k / 1236.85

    private fun julianEphemerisPurnama(k: Double): Double {
        val t = t(k)
        return 2451550.09766 + 29.530588861 * k + 0.00015437 * t * t -
            0.000000150 * t * t * t + 0.00000000073 * t * t * t * t
    }

    private fun tahunDesimal(tanggal: LocalDate): Double {
        val panjangTahun = if (tanggal.isLeapYear) 366.0 else 365.0
        return tanggal.year + (tanggal.dayOfYear - 1) / panjangTahun
    }

    private fun julianDayAwalTanggal(tanggal: LocalDate): Double {
        var y = tanggal.year
        var m = tanggal.monthValue
        val d = tanggal.dayOfMonth
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + d + b - 1524.5
    }

    private fun tanggalDariJulianDay(julianDay: Double): Pair<LocalDate, Double> {
        val z = floor(julianDay + 0.5)
        val f = julianDay + 0.5 - z
        var a = z
        if (z >= 2299161) {
            val alpha = floor((z - 1867216.25) / 36524.25)
            a = z + 1 + alpha - floor(alpha / 4.0)
        }
        val b = a + 1524
        val c = floor((b - 122.1) / 365.25)
        val d = floor(365.25 * c)
        val e = floor((b - d) / 30.6001)
        val dayDecimal = b - d - floor(30.6001 * e) + f
        val day = floor(dayDecimal).toInt()
        val month = if (e < 14) (e - 1).toInt() else (e - 13).toInt()
        val year = if (month > 2) (c - 4716).toInt() else (c - 4715).toInt()
        val jam = (dayDecimal - day) * 24.0
        return LocalDate.of(year, month, day) to jam
    }

    private fun jarakKeSimpul(argumenLintangDerajat: Double): Double {
        val normalized = normalisasiDerajat(argumenLintangDerajat)
        val keNodeNaik = abs(normalized)
        val keNodeTurun = abs(normalized - 180.0)
        val keSiklusBerikut = abs(360.0 - normalized)
        return minOf(keNodeNaik, keNodeTurun, keSiklusBerikut)
    }

    private fun normalisasiDerajat(value: Double): Double {
        var normalized = value % 360.0
        if (normalized < 0.0) normalized += 360.0
        return normalized
    }

    private data class KandidatPurnama(
        val julianDay: Double,
        val argumenLintangDerajat: Double,
        val selisihHari: Double,
    )
}
