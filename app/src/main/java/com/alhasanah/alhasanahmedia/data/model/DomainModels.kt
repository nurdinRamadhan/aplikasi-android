package com.alhasanah.alhasanahmedia.data.model

import kotlinx.serialization.Serializable

@Serializable
data class HafalanTahfidz(
    val id: Long,
    val santri_nis: String? = null,
    val tanggal: String? = null,
    val surat: String? = null,
    val ayat_awal: Int? = null,
    val ayat_akhir: Int? = null,
    val status: String? = null,
    val catatan: String? = null,
    val dicatat_oleh_id: String? = null,
    val total_hafalan: Int? = null,
    val hafalan_kitab: String? = null,
    val juz: Int? = null,
    val predikat: String? = null
)

@Serializable
data class MurojaahTahfidz(
    val id: Long,
    val santri_nis: String? = null,
    val tanggal: String? = null,
    val jenis_murojaah: String? = null,
    val juz: Int? = null,
    val surat: String? = null,
    val ayat_awal: Int? = null,
    val ayat_akhir: Int? = null,
    val halaman_awal: Int? = null,
    val halaman_akhir: Int? = null,
    val status: String? = null,
    val predikat: String? = null,
    val catatan: String? = null,
    val dicatat_oleh_id: String? = null
)

@Serializable
data class PrestasiSantri(
    val id: Long,
    val created_at: String? = null,
    val santri_nis: String,
    val kategori: String,
    val judul_prestasi: String,
    val keterangan: String? = null,
    val tanggal_prestasi: String? = null,
    val sertifikat_url: String? = null,
    val poin_prestasi: Int? = null,
    val dicatat_oleh_id: String? = null
)

@Serializable
data class PublicPrestasiSantri(
    val prestasi_id: Long,
    val santri_nama: String,
    val santri_kelas: String? = null,
    val santri_jurusan: String? = null,
    val kategori: String,
    val judul_prestasi: String,
    val keterangan: String? = null,
    val tanggal_prestasi: String? = null,
    val poin_prestasi: Int? = null
)

@Serializable
data class PublicPrestasiCategoryCount(
    val kategori: String,
    val total: Long
)

@Serializable
data class PelanggaranSantri(
    val id: Long,
    val santri_nis: String? = null,
    val tanggal: String? = null,
    val jenis_pelanggaran: String? = null,
    val poin: Int? = null,
    val hukuman: String? = null,
    val catatan: String? = null,
    val dicatat_oleh_id: String? = null
)

@Serializable
data class PerizinanSantri(
    val id: Long,
    val santri_nis: String? = null,
    val tanggal: String? = null,
    val tanggal_kembali: String? = null,
    val jenis_izin: String? = null,
    val keterangan: String? = null,
    val status: String? = null,
    val dicatat_oleh_id: String? = null
)

@Serializable
data class KesehatanSantri(
    val id: Long,
    val santri_nis: String? = null,
    val tanggal: String? = null,
    val keluhan: String? = null,
    val tindakan: String? = null,
    val catatan: String? = null,
    val dicatat_oleh_id: String? = null
)

@Serializable
data class HafalanKitab(
    val id: Long,
    val santri_nis: String,
    val tanggal: String,
    val nama_kitab: String,
    val bab_materi: String? = null,
    val bait_awal: Int? = null,
    val bait_akhir: Int? = null,
    val halaman_awal: Int? = null,
    val halaman_akhir: Int? = null,
    val predikat: String? = null,
    val status: String? = null,
    val catatan: String? = null
)

@Serializable
data class AbsensiHarianItem(
    val hari: String,
    val tanggal: String,
    val kegiatan: String,
    val sesi: String,
    val status: String
)

@Serializable
data class RingkasanAbsensiMingguan(
    val santri_nis: String,
    val santri_nama: String,
    val periode: String,
    val data: List<AbsensiHarianItem>
)
