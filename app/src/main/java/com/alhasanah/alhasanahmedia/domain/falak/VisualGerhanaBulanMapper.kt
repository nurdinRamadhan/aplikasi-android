package com.alhasanah.alhasanahmedia.domain.falak

import kotlin.math.abs

class VisualGerhanaBulanMapper {

    fun map(hasil: HasilGerhanaBulanEphemeris): VisualGerhanaBulan {
        val radiusBulan = 0.075
        val radiusUmbra = (hasil.bayanganBumi.semiDiameterBayanganIntiDerajat /
            hasil.dataInterpolasi.semiDiameterBulan.hasilDerajat * radiusBulan)
            .coerceIn(0.12, 0.34)
        val radiusPenumbra = (radiusUmbra * 1.72).coerceIn(radiusUmbra + 0.08, 0.46)
        val zScale = radiusBulan / hasil.dataInterpolasi.semiDiameterBulan.hasilDerajat.coerceAtLeast(0.0001)
        val y = (0.5 - hasil.simpul.zDerajat * zScale).coerceIn(0.18, 0.82)
        val pusat = TitikVisualFalak(0.5, 0.5)
        val lintasanAwal = TitikVisualFalak((0.5 - radiusPenumbra - radiusBulan - 0.08).coerceAtLeast(0.04), y)
        val lintasanAkhir = TitikVisualFalak((0.5 + radiusPenumbra + radiusBulan + 0.08).coerceAtMost(0.96), y)
        val bulanTengah = TitikVisualFalak(0.5, y)
        return VisualGerhanaBulan(
            judul = "Visual Gerhana Bulan",
            sumberData = "Ephemeris Kemenag",
            jenis = hasil.kesimpulan.jenis,
            penumbra = LingkaranVisualFalak(pusat, radiusPenumbra),
            umbra = LingkaranVisualFalak(pusat, radiusUmbra),
            bulan = LingkaranVisualFalak(bulanTengah, radiusBulan),
            lintasanAwal = lintasanAwal,
            lintasanAkhir = lintasanAkhir,
            kontak = listOf(
                KontakVisualGerhanaBulan("Mulai", hasil.waktuKontak.mulaiGerhanaLokal, posisiKontak(lintasanAwal, bulanTengah, 0.25, hasil.waktuKontak.mulaiGerhanaLokal)),
                KontakVisualGerhanaBulan("Total Awal", hasil.waktuKontak.mulaiTotalLokal, posisiKontak(lintasanAwal, bulanTengah, 0.62, hasil.waktuKontak.mulaiTotalLokal)),
                KontakVisualGerhanaBulan("Tengah", hasil.waktuKontak.tengahGerhanaLokal, bulanTengah),
                KontakVisualGerhanaBulan("Total Akhir", hasil.waktuKontak.selesaiTotalLokal, posisiKontak(bulanTengah, lintasanAkhir, 0.38, hasil.waktuKontak.selesaiTotalLokal)),
                KontakVisualGerhanaBulan("Selesai", hasil.waktuKontak.selesaiGerhanaLokal, posisiKontak(bulanTengah, lintasanAkhir, 0.75, hasil.waktuKontak.selesaiGerhanaLokal)),
            ),
            magnitudeUmbra = hasil.magnitude.magnitudeUmbra,
            status = hasil.kesimpulan.status,
        )
    }

    private fun posisiKontak(
        awal: TitikVisualFalak,
        akhir: TitikVisualFalak,
        fraksi: Double,
        waktu: WaktuFalak?,
    ): TitikVisualFalak? {
        if (waktu == null) return null
        return TitikVisualFalak(
            x = awal.x + (akhir.x - awal.x) * fraksi,
            y = awal.y + (akhir.y - awal.y) * abs(fraksi),
        )
    }
}
