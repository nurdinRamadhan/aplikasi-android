package com.alhasanah.alhasanahmedia.data.model.falak

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class FalakPaketDataDto(
    val id: String,
    val kode: String,
    val judul: String,
    val deskripsi: String? = null,
    val tahun: Int,
    val versi: String,
    @SerialName("jenis_sumber") val jenisSumber: String,
    @SerialName("sumber_resmi") val sumberResmi: String,
    @SerialName("zona_waktu_data") val zonaWaktuData: String = "UT",
    val status: String,
    @SerialName("path_manifest_storage") val pathManifestStorage: String? = null,
    @SerialName("sha256_pdf") val sha256Pdf: String? = null,
    @SerialName("sha256_manifest") val sha256Manifest: String? = null,
    @SerialName("jumlah_halaman") val jumlahHalaman: Int = 0,
    @SerialName("jumlah_hari_ephemeris") val jumlahHariEphemeris: Int = 0,
    @SerialName("jumlah_tabel_hilal") val jumlahTabelHilal: Int = 0,
    @SerialName("jumlah_baris_indeks") val jumlahBarisIndeks: Int = 0,
    @SerialName("tanggal_mulai") val tanggalMulai: String? = null,
    @SerialName("tanggal_selesai") val tanggalSelesai: String? = null,
)

@Serializable
data class FalakManifest(
    @SerialName("schema_version") val schemaVersion: Int,
    val kode: String,
    val judul: String,
    val tahun: Int,
    val versi: String,
    @SerialName("jenis_sumber") val jenisSumber: String,
    @SerialName("sumber_resmi") val sumberResmi: String,
    val bucket: String,
    @SerialName("storage_prefix") val storagePrefix: String,
    @SerialName("zona_waktu_data") val zonaWaktuData: String,
    @SerialName("rentang_tanggal") val rentangTanggal: FalakRentangTanggal,
    val jumlah: FalakManifestJumlah,
    val berkas: List<FalakBerkasManifest>,
    val catatan: List<String> = emptyList(),
)

@Serializable
data class FalakRentangTanggal(
    val mulai: String? = null,
    val selesai: String? = null,
)

@Serializable
data class FalakManifestJumlah(
    @SerialName("halaman_pdf") val halamanPdf: Int = 0,
    @SerialName("hari_ephemeris") val hariEphemeris: Int = 0,
    @SerialName("tabel_hilal") val tabelHilal: Int = 0,
    @SerialName("baris_indeks") val barisIndeks: Int = 0,
)

@Serializable
data class FalakBerkasManifest(
    @SerialName("jenis_berkas") val jenisBerkas: String,
    @SerialName("nama_berkas") val namaBerkas: String,
    @SerialName("nama_tampil") val namaTampil: String,
    @SerialName("path_storage") val pathStorage: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("ukuran_bytes") val ukuranBytes: Long,
    val sha256: String,
    @SerialName("jumlah_record") val jumlahRecord: Int = 0,
    @SerialName("wajib_diunduh") val wajibDiunduh: Boolean = false,
    val urutan: Int = 0,
    val status: String = "aktif",
)

@Serializable
data class FalakEphemerisHarianFile(
    @SerialName("ephemeris_harian") val ephemerisHarian: List<FalakEphemerisHarian> = emptyList(),
)

@Serializable
data class FalakEphemerisHarian(
    val page: Int? = null,
    val date: String,
    @SerialName("has_structured_hourly_table") val hasStructuredHourlyTable: Boolean = false,
    @SerialName("hourly_table") val hourlyTable: FalakHourlyTable = FalakHourlyTable(),
    @SerialName("raw_text") val rawText: String? = null,
)

@Serializable
data class FalakHourlyTable(
    val sun: List<JsonObject> = emptyList(),
    val moon: List<JsonObject> = emptyList(),
)

@Serializable
data class FalakHilalLokasiFile(
    @SerialName("hilal_lokasi") val hilalLokasi: List<FalakHilalTable> = emptyList(),
)

@Serializable
data class FalakHilalTable(
    val page: Int? = null,
    @SerialName("event_date_raw") val eventDateRaw: String? = null,
    @SerialName("hijri_month_raw") val hijriMonthRaw: String? = null,
    @SerialName("ijtima_raw") val ijtimaRaw: String? = null,
    val rows: List<JsonObject> = emptyList(),
    @SerialName("raw_text") val rawText: String? = null,
)

@Serializable
data class FalakIndeksFile(
    @SerialName("indeks_pencarian") val indeksPencarian: List<FalakIndeksItem> = emptyList(),
)

@Serializable
data class FalakIndeksItem(
    @SerialName("tipe_indeks") val tipeIndeks: String,
    val judul: String,
    val ringkasan: String? = null,
    @SerialName("kata_kunci") val kataKunci: List<String> = emptyList(),
    @SerialName("tanggal_data") val tanggalData: String? = null,
    @SerialName("jam_ut") val jamUt: Int? = null,
    @SerialName("nama_lokasi") val namaLokasi: String? = null,
    @SerialName("nomor_halaman_pdf") val nomorHalamanPdf: Int? = null,
    @SerialName("path_json_pointer") val pathJsonPointer: String? = null,
    val metadata: JsonObject? = null,
)

@Serializable
data class FalakHalamanPdfFile(
    @SerialName("halaman_pdf") val halamanPdf: List<FalakHalamanPdf> = emptyList(),
)

@Serializable
data class FalakHalamanPdf(
    val page: Int,
    val text: String,
)

data class FalakCacheStatus(
    val isReady: Boolean,
    val paket: FalakPaketDataDto?,
    val manifest: FalakManifest?,
    val localPath: String?,
)

data class FalakDataLengkap(
    val paket: FalakPaketDataDto,
    val manifest: FalakManifest,
    val ephemerisHarian: List<FalakEphemerisHarian>,
    val hilalLokasi: List<FalakHilalTable>,
    val indeks: List<FalakIndeksItem>,
)

fun JsonObject.textAt(name: String): String? = this[name]?.jsonPrimitiveContentOrNull()
fun JsonObject.numberAt(name: String): Double? = this[name]?.jsonNumberContentOrNull()

private fun JsonElement.jsonPrimitiveContentOrNull(): String? =
    runCatching { jsonPrimitive.content }.getOrNull()

private fun JsonElement.jsonNumberContentOrNull(): Double? =
    runCatching { jsonPrimitive.doubleOrNull ?: jsonPrimitive.content.replace(",", ".").toDoubleOrNull() }.getOrNull()
