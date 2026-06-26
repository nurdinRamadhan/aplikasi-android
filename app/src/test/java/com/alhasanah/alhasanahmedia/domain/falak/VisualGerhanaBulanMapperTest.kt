package com.alhasanah.alhasanahmedia.domain.falak

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualGerhanaBulanMapperTest {

    @Test
    fun mapMenjagaKoordinatDalamKanvasNormal() {
        val hasil = GerhanaBulanEphemerisCalculator().hitung(
            konteks = contohKonteksGerhana(),
            ephemerisHarian = contohEphemerisGerhanaBulan(),
        )

        val visual = VisualGerhanaBulanMapper().map(hasil)

        assertEquals(JenisGerhanaBulan.Total, visual.jenis)
        assertTrue(visual.penumbra.radius > visual.umbra.radius)
        assertTrue(visual.umbra.radius > visual.bulan.radius)
        assertTrue(visual.bulan.pusat.x in 0.0..1.0)
        assertTrue(visual.bulan.pusat.y in 0.0..1.0)
        assertTrue(visual.kontak.any { it.label == "Tengah" && it.posisi != null })
    }
}
