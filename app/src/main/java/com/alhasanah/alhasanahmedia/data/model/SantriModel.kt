package com.alhasanah.alhasanahmedia.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SantriModel(
    @SerialName("nis")
    val id: String,

    @SerialName("nama")
    val namaLengkap: String,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null,

    @SerialName("kelas")
    val kelas: String? = null,

    @SerialName("jurusan")
    val jurusan: String? = null,

    @SerialName("pembimbing")
    val pembimbing: String? = null,

    @SerialName("foto_url")
    val fotoUrl: String? = null,

    @SerialName("status_spp")
    val statusSpp: String? = null,

    @SerialName("nik")
    val nik: String? = null,

    @SerialName("nisn")
    val nisn: String? = null,

    @SerialName("no_kk")
    val noKk: String? = null,

    @SerialName("tempat_lahir")
    val tempatLahir: String? = null,

    @SerialName("tanggal_lahir")
    val tanggalLahir: String? = null, // Format: YYYY-MM-DD

    @SerialName("ayah")
    val namaAyah: String? = null,

    @SerialName("ibu")
    val namaIbu: String? = null,

    @SerialName("no_kontak_wali")
    val noKontakWali: String? = null,

    @SerialName("alamat_lengkap")
    val alamatLengkap: String? = null,

    @SerialName("anak_ke")
    val anakKe: String? = null,

    @SerialName("wali_id")
    val waliId: String? = null,

    @SerialName("jenis_kelamin")
    val jenisKelamin: String? = null, // Laki-laki or Perempuan

    @SerialName("status_santri")
    val statusSantri: String? = null,

    @SerialName("status_mukim")
    val statusMukim: String? = null,

    @SerialName("tahun_masuk")
    val tahunMasuk: Int? = null,

    @SerialName("tanggal_masuk")
    val tanggalMasuk: String? = null,

    @SerialName("tahun_lulus_keluar")
    val tahunLulusKeluar: Int? = null,

    @SerialName("tanggal_lulus_keluar")
    val tanggalLulusKeluar: String? = null,

    @SerialName("alasan_keluar")
    val alasanKeluar: String? = null,

    @SerialName("nsp")
    val nsp: String? = null,

    @SerialName("agama")
    val agama: String? = null,

    @SerialName("kewarganegaraan")
    val kewarganegaraan: String? = null,

    @SerialName("rt")
    val rt: String? = null,

    @SerialName("rw")
    val rw: String? = null,

    @SerialName("desa_kelurahan")
    val desaKelurahan: String? = null,

    @SerialName("kecamatan_id")
    val kecamatanId: String? = null,

    @SerialName("kabupaten_kota")
    val kabupatenKota: String? = null,

    @SerialName("provinsi")
    val provinsi: String? = null,

    @SerialName("kode_pos")
    val kodePos: String? = null,

    @SerialName("jarak_rumah_km")
    val jarakRumahKm: Double? = null,

    @SerialName("latitude")
    val latitude: Double? = null,

    @SerialName("longitude")
    val longitude: Double? = null,

    @SerialName("geocode_status")
    val geocodeStatus: String? = null,

    @SerialName("geocode_provider")
    val geocodeProvider: String? = null,

    @SerialName("geocode_confidence")
    val geocodeConfidence: Double? = null,

    @SerialName("geocoded_at")
    val geocodedAt: String? = null,

    @SerialName("nik_ayah")
    val nikAyah: String? = null,

    @SerialName("status_ayah")
    val statusAyah: String? = null,

    @SerialName("pendidikan_ayah")
    val pendidikanAyah: String? = null,

    @SerialName("pekerjaan_ayah")
    val pekerjaanAyah: String? = null,

    @SerialName("penghasilan_ayah")
    val penghasilanAyah: String? = null,

    @SerialName("nik_ibu")
    val nikIbu: String? = null,

    @SerialName("status_ibu")
    val statusIbu: String? = null,

    @SerialName("pendidikan_ibu")
    val pendidikanIbu: String? = null,

    @SerialName("pekerjaan_ibu")
    val pekerjaanIbu: String? = null,

    @SerialName("penghasilan_ibu")
    val penghasilanIbu: String? = null,

    @SerialName("nama_wali")
    val namaWali: String? = null,

    @SerialName("nik_wali")
    val nikWali: String? = null,

    @SerialName("hubungan_wali")
    val hubunganWali: String? = null,

    @SerialName("pendidikan_wali")
    val pendidikanWali: String? = null,

    @SerialName("pekerjaan_wali")
    val pekerjaanWali: String? = null,

    @SerialName("penghasilan_wali")
    val penghasilanWali: String? = null,

    @SerialName("no_kip")
    val noKip: String? = null,

    @SerialName("penerima_pip")
    val penerimaPip: Boolean? = null,

    @SerialName("penerima_beasiswa")
    val penerimaBeasiswa: Boolean? = null,

    @SerialName("jenis_beasiswa")
    val jenisBeasiswa: String? = null,

    @SerialName("kebutuhan_khusus")
    val kebutuhanKhusus: String? = null,

    @SerialName("emis_extra")
    val emisExtra: JsonObject? = null,

    @SerialName("hafalan_kitab")
    val hafalanKitab: String? = null,

    @SerialName("total_hafalan")
    val totalHafalan: String? = null
)
