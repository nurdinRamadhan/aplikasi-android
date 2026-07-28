package com.alhasanah.alhasanahmedia.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LaporanMasalah(
    val id: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("nama_pengguna") val namaPengguna: String? = null,
    val nis: String? = null,
    val judul: String,
    val deskripsi: String,
    val kategori: String = "BUG",
    val prioritas: String = "MEDIUM",
    val status: String = "OPEN",
    @SerialName("app_version") val appVersion: String? = null,
    @SerialName("android_version") val androidVersion: String? = null,
    @SerialName("device_brand") val deviceBrand: String? = null,
    @SerialName("device_model") val deviceModel: String? = null,
    @SerialName("device_manufacturer") val deviceManufacturer: String? = null,
    @SerialName("device_sdk") val deviceSdk: Int? = null,
    val locale: String? = null,
    val timezone: String? = null,
    @SerialName("screenshot_url") val screenshotUrl: String? = null,
    @SerialName("attachment_paths") val attachmentPaths: List<String>? = null,
    @SerialName("admin_note") val adminNote: String? = null,
    @SerialName("fixed_at") val fixedAt: String? = null,
    @SerialName("fixed_by") val fixedBy: String? = null,
    val source: String = "android",
    @SerialName("is_public") val isPublic: Boolean = false
)

@Serializable
data class LaporanMasalahLog(
    val id: Long? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("laporan_id") val laporanId: String,
    @SerialName("old_status") val oldStatus: String? = null,
    @SerialName("new_status") val newStatus: String,
    @SerialName("changed_by") val changedBy: String? = null,
    val catatan: String? = null
)

@Serializable
data class LaporanMasalahInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("nama_pengguna") val namaPengguna: String? = null,
    val nis: String? = null,
    val judul: String,
    val deskripsi: String,
    val kategori: String = "BUG",
    val prioritas: String = "MEDIUM",
    @SerialName("app_version") val appVersion: String? = null,
    @SerialName("android_version") val androidVersion: String? = null,
    @SerialName("device_brand") val deviceBrand: String? = null,
    @SerialName("device_model") val deviceModel: String? = null,
    @SerialName("device_manufacturer") val deviceManufacturer: String? = null,
    @SerialName("device_sdk") val deviceSdk: Int? = null,
    val locale: String? = null,
    val timezone: String? = null,
    @SerialName("screenshot_url") val screenshotUrl: String? = null,
    val source: String = "android"
)

enum class LaporanKategori(val label: String) {
    BUG("Bug"),
    FITUR("Usulan Fitur"),
    PERTANYAAN("Pertanyaan"),
    MASUKAN("Masukan")
}

enum class LaporanPrioritas(val label: String) {
    LOW("Rendah"),
    MEDIUM("Sedang"),
    HIGH("Tinggi"),
    URGENT("Mendesak")
}

enum class LaporanStatus(val label: String) {
    OPEN("Baru"),
    IN_PROGRESS("Dalam Proses"),
    FIXED("Selesai"),
    REJECTED("Ditolak"),
    NEED_INFO("Butuh Info"),
    WONT_FIX("Tidak Diperbaiki")
}
