package com.alhasanah.alhasanahmedia.domain.falak

import kotlin.math.abs

class VisualHilalMapper {

    fun map(hasil: HasilHisabHilalEphemeris): VisualHilal {
        val tinggi = hasil.kesimpulan.tinggiHilalMariDerajat
        val elongasi = hasil.kesimpulan.elongasiHilalDerajat
        val selisihAzimut = hasil.kesimpulan.azimutHilalDerajat - hasil.kesimpulan.azimutMatahariDerajat
        val ufukY = 0.72
        val matahariY = (ufukY + 0.16).coerceAtMost(0.92)
        val bulanY = (ufukY - tinggi / 18.0).coerceIn(0.12, 0.88)
        val bulanX = (0.5 + selisihAzimut / 36.0).coerceIn(0.16, 0.84)
        return VisualHilal(
            judul = "Visual Hilal",
            sumberData = "Ephemeris Kemenag",
            tinggiHilalDerajat = tinggi,
            elongasiDerajat = elongasi,
            azimutMatahariDerajat = hasil.kesimpulan.azimutMatahariDerajat,
            azimutBulanDerajat = hasil.kesimpulan.azimutHilalDerajat,
            fractionIlluminationPersen = hasil.kesimpulan.fractionIlluminationPersen,
            nurulHilalJari = hasil.kesimpulan.cahayaHilalJari,
            memenuhiKriteria = hasil.kesimpulan.memenuhiKriteria,
            status = hasil.kesimpulan.statusPrakiraan,
            matahari = TitikVisualFalak(0.5, matahariY),
            bulan = TitikVisualFalak(bulanX, bulanY),
            ufukY = ufukY,
        )
    }

    fun ketebalanHilal(visual: VisualHilal): Double =
        (0.08 + abs(visual.fractionIlluminationPersen) / 100.0 * 0.34).coerceIn(0.08, 0.42)
}
