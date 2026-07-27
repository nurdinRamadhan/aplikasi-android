package com.alhasanah.alhasanahmedia.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AbsensiLengkapResponse(
    @SerialName("santri_nis") val santriNis: String = "",
    @SerialName("santri_nama") val santriNama: String = "",
    @SerialName("kelas") val kelas: String = "",
    @SerialName("periode") val periode: String = "",
    @SerialName("start_date") val startDate: String = "",
    @SerialName("end_date") val endDate: String = "",
    @SerialName("total_hari") val totalHari: Int = 0,
    @SerialName("ringkasan") val ringkasan: RingkasanSummary = RingkasanSummary(),
    @SerialName("per_kegiatan") val perKegiatan: List<RingkasanKegiatan> = emptyList(),
    @SerialName("per_hari") val perHari: List<HariAbsensi> = emptyList()
)

@Serializable
data class RingkasanSummary(
    @SerialName("hadir") val hadir: Int = 0,
    @SerialName("izin") val izin: Int = 0,
    @SerialName("sakit") val sakit: Int = 0,
    @SerialName("alpha") val alpha: Int = 0,
    @SerialName("sekolah") val sekolah: Int = 0,
    @SerialName("pulang") val pulang: Int = 0,
    @SerialName("total") val total: Int = 0,
    @SerialName("persentase") val persentase: Double = 0.0
)

@Serializable
data class RingkasanKegiatan(
    @SerialName("nama") val nama: String = "",
    @SerialName("hadir") val hadir: Int = 0,
    @SerialName("izin") val izin: Int = 0,
    @SerialName("sakit") val sakit: Int = 0,
    @SerialName("alpha") val alpha: Int = 0,
    @SerialName("total") val total: Int = 0,
    @SerialName("persentase") val persentase: Double = 0.0
)

@Serializable
data class HariAbsensi(
    @SerialName("tanggal") val tanggal: String = "",
    @SerialName("tanggal_display") val tanggalDisplay: String = "",
    @SerialName("hari") val hari: String = "",
    @SerialName("hari_singkat") val hariSingkat: String = "",
    @SerialName("kegiatan") val kegiatan: List<KegiatanHarian> = emptyList()
)

@Serializable
data class KegiatanHarian(
    @SerialName("nama") val nama: String = "",
    @SerialName("kategori") val kategori: String = "",
    @SerialName("sesi") val sesi: String = "",
    @SerialName("status") val status: String = "",
    @SerialName("sumber") val sumber: String = ""
)

@Serializable
enum class QuickFilter {
    HARI_INI,
    KEMARIN,
    _7_HARI,
    _30_HARI,
    CUSTOM
}

@Serializable
enum class ViewMode(val label: String) {
    HARIAN("Harian"),
    SEMUA_KEGIATAN("Semua Kegiatan")
}

@Serializable
enum class StatusAbsensi(val label: String, val colorHex: Long) {
    HADIR("Hadir", 0xFF16A34A),
    IZIN("Izin", 0xFFF59E0B),
    SAKIT("Sakit", 0xFFEF4444),
    ALPHA("Alpha", 0xFF6B7280),
    SEKOLAH("Sekolah", 0xFF8B5CF6),
    PULANG("Pulang", 0xFF0891B2);

    companion object {
        fun fromString(value: String): StatusAbsensi {
            return when (value.uppercase()) {
                "HADIR" -> HADIR
                "IZIN" -> IZIN
                "SAKIT" -> SAKIT
                "ALFA", "GHAIB" -> ALPHA
                "SEKOLAH" -> SEKOLAH
                "PULANG" -> PULANG
                else -> ALPHA
            }
        }
    }
}