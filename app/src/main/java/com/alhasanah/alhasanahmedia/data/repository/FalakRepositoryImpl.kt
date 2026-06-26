package com.alhasanah.alhasanahmedia.data.repository

import android.content.Context
import com.alhasanah.alhasanahmedia.BuildConfig
import com.alhasanah.alhasanahmedia.data.model.falak.FalakCacheStatus
import com.alhasanah.alhasanahmedia.data.model.falak.FalakDataLengkap
import com.alhasanah.alhasanahmedia.data.model.falak.FalakEphemerisHarian
import com.alhasanah.alhasanahmedia.data.model.falak.FalakEphemerisHarianFile
import com.alhasanah.alhasanahmedia.data.model.falak.FalakHalamanPdf
import com.alhasanah.alhasanahmedia.data.model.falak.FalakHalamanPdfFile
import com.alhasanah.alhasanahmedia.data.model.falak.FalakHilalLokasiFile
import com.alhasanah.alhasanahmedia.data.model.falak.FalakHilalTable
import com.alhasanah.alhasanahmedia.data.model.falak.FalakIndeksFile
import com.alhasanah.alhasanahmedia.data.model.falak.FalakIndeksItem
import com.alhasanah.alhasanahmedia.data.model.falak.FalakManifest
import com.alhasanah.alhasanahmedia.data.model.falak.FalakPaketDataDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import java.io.File
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class FalakRepositoryImpl(
    context: Context,
    private val postgrest: Postgrest,
    private val httpClient: OkHttpClient,
) : FalakRepository {

    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val cacheRoot = File(appContext.filesDir, "falak/ephemeris")
    private val statusFlow = MutableStateFlow(readCacheStatus())

    private var memoryData: FalakDataLengkap? = null
    private val memoryDataByYear = mutableMapOf<Int, FalakDataLengkap>()
    private var halamanPdfCache: List<FalakHalamanPdf>? = null

    override fun observeCacheStatus(): Flow<FalakCacheStatus> = statusFlow

    override suspend fun refreshPaketKemenag(tahun: Int): Result<FalakDataLengkap> = withContext(Dispatchers.IO) {
        runCatching {
            val paket = fetchPaketAktif(tahun) ?: error("Paket Falak Kemenag $tahun belum aktif.")
            val dir = File(cacheRoot, paket.kode).apply { mkdirs() }
            val manifestPath = File(dir, "manifest.json")
            val needsManifest = !manifestPath.exists() ||
                paket.sha256Manifest?.let { sha256(manifestPath) != it } == true

            if (needsManifest) {
                downloadToFile(publicObjectUrl(paket.pathManifestStorage ?: error("Path manifest kosong.")), manifestPath)
            }

            val manifest = json.decodeFromString<FalakManifest>(manifestPath.readText())
            for (berkas in manifest.berkas) {
                val local = File(dir, berkas.namaBerkas)
                val valid = local.exists() && local.length() == berkas.ukuranBytes && sha256(local) == berkas.sha256
                if (!valid) {
                    downloadToFile(publicObjectUrl(berkas.pathStorage), local)
                    val afterDownloadValid = local.length() == berkas.ukuranBytes && sha256(local) == berkas.sha256
                    check(afterDownloadValid) { "Checksum gagal untuk ${berkas.namaBerkas}." }
                }
            }
            val data = readDataLengkap(paket, manifest, dir)
            memoryData = data
            memoryDataByYear[paket.tahun] = data
            statusFlow.value = FalakCacheStatus(true, paket, manifest, dir.absolutePath)
            data
        }
    }

    override suspend fun loadDataLengkap(): Result<FalakDataLengkap> = withContext(Dispatchers.IO) {
        runCatching {
            memoryData ?: readLocalDataLengkap().also {
                memoryData = it
                memoryDataByYear[it.paket.tahun] = it
            }
        }
    }

    override suspend fun loadDataLengkap(tahun: Int): Result<FalakDataLengkap> = withContext(Dispatchers.IO) {
        runCatching {
            memoryDataByYear[tahun] ?: readLocalDataLengkap(tahun).also {
                memoryData = it
                memoryDataByYear[tahun] = it
            }
        }
    }

    override suspend fun cariIndeks(query: String, tipe: String?, limit: Int): List<FalakIndeksItem> {
        val data = loadDataLengkap().getOrNull() ?: return emptyList()
        val normalized = query.trim().lowercase()
        return data.indeks.asSequence()
            .filter { tipe == null || it.tipeIndeks == tipe }
            .filter {
                normalized.isBlank() ||
                    it.judul.lowercase().contains(normalized) ||
                    it.ringkasan.orEmpty().lowercase().contains(normalized) ||
                    it.namaLokasi.orEmpty().lowercase().contains(normalized) ||
                    it.tanggalData.orEmpty().contains(normalized) ||
                    it.kataKunci.any { keyword -> keyword.lowercase().contains(normalized) }
            }
            .take(limit)
            .toList()
    }

    override suspend fun getEphemerisTanggal(tanggal: String): FalakEphemerisHarian? =
        loadDataLengkap().getOrNull()?.ephemerisHarian?.firstOrNull { it.date == tanggal }

    override suspend fun getHilalTable(index: Int): FalakHilalTable? =
        loadDataLengkap().getOrNull()?.hilalLokasi?.getOrNull(index)

    override suspend fun getHalamanPdf(nomorHalaman: Int): FalakHalamanPdf? = withContext(Dispatchers.IO) {
        val data = loadDataLengkap().getOrNull() ?: return@withContext null
        val dir = File(cacheRoot, data.paket.kode)
        val pages = halamanPdfCache ?: readHalamanPdf(dir).also { halamanPdfCache = it }
        pages.firstOrNull { it.page == nomorHalaman }
    }

    private suspend fun fetchPaketAktif(tahun: Int): FalakPaketDataDto? {
        return postgrest.from("falak_paket_data").select {
            filter {
                eq("status", "aktif")
                eq("jenis_sumber", "kemenag")
                eq("tahun", tahun)
            }
            limit(1)
        }.decodeSingleOrNull<FalakPaketDataDto>()
    }

    private fun readLocalDataLengkap(tahun: Int? = null): FalakDataLengkap {
        val paketDir = cacheRoot.listFiles()?.firstOrNull { dir ->
            val manifestFile = File(dir, "manifest.json")
            if (!manifestFile.exists()) {
                false
            } else if (tahun == null) {
                true
            } else {
                runCatching {
                    json.decodeFromString<FalakManifest>(manifestFile.readText()).tahun == tahun
                }.getOrDefault(false)
            }
        }
            ?: error("Cache Falak belum tersedia.")
        val manifest = json.decodeFromString<FalakManifest>(File(paketDir, "manifest.json").readText())
        val paket = FalakPaketDataDto(
            id = "",
            kode = manifest.kode,
            judul = manifest.judul,
            tahun = manifest.tahun,
            versi = manifest.versi,
            jenisSumber = manifest.jenisSumber,
            sumberResmi = manifest.sumberResmi,
            zonaWaktuData = manifest.zonaWaktuData,
            status = "aktif",
            pathManifestStorage = "${manifest.storagePrefix}/manifest.json",
            jumlahHalaman = manifest.jumlah.halamanPdf,
            jumlahHariEphemeris = manifest.jumlah.hariEphemeris,
            jumlahTabelHilal = manifest.jumlah.tabelHilal,
            jumlahBarisIndeks = manifest.jumlah.barisIndeks,
            tanggalMulai = manifest.rentangTanggal.mulai,
            tanggalSelesai = manifest.rentangTanggal.selesai,
        )
        return readDataLengkap(paket, manifest, paketDir)
    }

    private fun readDataLengkap(paket: FalakPaketDataDto, manifest: FalakManifest, dir: File): FalakDataLengkap {
        val ephemeris = json.decodeFromString<FalakEphemerisHarianFile>(
            File(dir, "ephemeris-harian.json").readText()
        ).ephemerisHarian
        val hilal = json.decodeFromString<FalakHilalLokasiFile>(
            File(dir, "hilal-lokasi.json").readText()
        ).hilalLokasi
        val indeks = json.decodeFromString<FalakIndeksFile>(
            File(dir, "indeks-pencarian.json").readText()
        ).indeksPencarian
        return FalakDataLengkap(paket, manifest, ephemeris, hilal, indeks)
    }

    private fun readHalamanPdf(dir: File): List<FalakHalamanPdf> {
        val file = File(dir, "halaman-pdf.json.gz")
        return GZIPInputStream(file.inputStream()).bufferedReader().use { reader ->
            json.decodeFromString<FalakHalamanPdfFile>(reader.readText()).halamanPdf
        }
    }

    private fun readCacheStatus(): FalakCacheStatus {
        val paketDir = cacheRoot.listFiles()?.firstOrNull { File(it, "manifest.json").exists() }
            ?: return FalakCacheStatus(false, null, null, null)
        val manifest = runCatching {
            json.decodeFromString<FalakManifest>(File(paketDir, "manifest.json").readText())
        }.getOrNull() ?: return FalakCacheStatus(false, null, null, paketDir.absolutePath)
        val paket = FalakPaketDataDto(
            id = "",
            kode = manifest.kode,
            judul = manifest.judul,
            tahun = manifest.tahun,
            versi = manifest.versi,
            jenisSumber = manifest.jenisSumber,
            sumberResmi = manifest.sumberResmi,
            zonaWaktuData = manifest.zonaWaktuData,
            status = "aktif",
            pathManifestStorage = "${manifest.storagePrefix}/manifest.json",
            jumlahHalaman = manifest.jumlah.halamanPdf,
            jumlahHariEphemeris = manifest.jumlah.hariEphemeris,
            jumlahTabelHilal = manifest.jumlah.tabelHilal,
            jumlahBarisIndeks = manifest.jumlah.barisIndeks,
            tanggalMulai = manifest.rentangTanggal.mulai,
            tanggalSelesai = manifest.rentangTanggal.selesai,
        )
        return FalakCacheStatus(true, paket, manifest, paketDir.absolutePath)
    }

    private fun publicObjectUrl(path: String): String {
        val base = BuildConfig.SUPABASE_URL.trimEnd('/')
        return "$base/storage/v1/object/public/falak-ephemeris/$path"
    }

    private fun downloadToFile(url: String, destination: File) {
        destination.parentFile?.mkdirs()
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Unduh gagal ${response.code}: $url" }
            val body = response.body ?: error("Response kosong: $url")
            destination.outputStream().use { output -> body.byteStream().copyTo(output) }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
