package com.alhasanah.alhasanahmedia.domain.falak

data class TitikVisualFalak(
    val x: Double,
    val y: Double,
)

data class LingkaranVisualFalak(
    val pusat: TitikVisualFalak,
    val radius: Double,
)

data class KontakVisualGerhanaBulan(
    val label: String,
    val waktu: WaktuFalak?,
    val posisi: TitikVisualFalak?,
)

data class VisualGerhanaBulan(
    val judul: String,
    val sumberData: String,
    val jenis: JenisGerhanaBulan,
    val penumbra: LingkaranVisualFalak,
    val umbra: LingkaranVisualFalak,
    val bulan: LingkaranVisualFalak,
    val lintasanAwal: TitikVisualFalak,
    val lintasanAkhir: TitikVisualFalak,
    val kontak: List<KontakVisualGerhanaBulan>,
    val magnitudeUmbra: Double?,
    val status: String,
)

data class VisualHilal(
    val judul: String,
    val sumberData: String,
    val tinggiHilalDerajat: Double,
    val elongasiDerajat: Double,
    val azimutMatahariDerajat: Double,
    val azimutBulanDerajat: Double,
    val fractionIlluminationPersen: Double,
    val nurulHilalJari: Double,
    val memenuhiKriteria: Boolean,
    val status: String,
    val matahari: TitikVisualFalak,
    val bulan: TitikVisualFalak,
    val ufukY: Double,
)
