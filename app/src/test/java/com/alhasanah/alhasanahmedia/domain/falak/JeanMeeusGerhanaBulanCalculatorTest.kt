package com.alhasanah.alhasanahmedia.domain.falak

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JeanMeeusGerhanaBulanCalculatorTest {

    @Test
    fun prakiraanPurnamaTerdekatRamadan1447DekatTanggalAcuanKemenag() {
        val result = JeanMeeusGerhanaBulanCalculator()
            .prakiraanPurnamaTerdekat(LocalDate.parse("2026-03-04"))

        assertEquals(LocalDate.parse("2026-03-03"), result.tanggalPurnamaTerdekatUt)
        assertTrue(result.selisihHariDariInput in -2.0..1.0)
        assertTrue(result.jarakKeSimpulDerajat < 13.9)
        assertTrue(result.memungkinkanGerhana)
    }
}
