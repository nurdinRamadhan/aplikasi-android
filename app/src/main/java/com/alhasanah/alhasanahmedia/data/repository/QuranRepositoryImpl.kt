package com.alhasanah.alhasanahmedia.data.repository

import android.util.Log
import com.alhasanah.alhasanahmedia.data.model.quran.*
import com.alhasanah.alhasanahmedia.data.remote.quran.QuranApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap

class QuranRepositoryImpl(private val apiService: QuranApiService) : QuranRepository {

    private val TAG = "QuranRepository"
    private val surahCache = ConcurrentHashMap<Int, SurahDetail>()
    private val juzCache = ConcurrentHashMap<Int, JuzDetail>()

    override fun getSurahList(): Flow<Result<List<SurahListItem>>> = flow {
        try {
            val response = apiService.getAllSurah()
            if (response.status) {
                emit(Result.success(response.data))
            } else {
                emit(Result.failure(Exception("Gagal mengambil daftar surah")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun getSurahDetail(nomor: Int): Flow<Result<SurahDetail>> = flow {
        surahCache[nomor]?.let {
            emit(Result.success(it))
            return@flow
        }

        try {
            var currentPage = 1
            val allAyahs = mutableListOf<Ayah>()
            var surahDetail: SurahDetail? = null
            
            do {
                val response = apiService.getSurahDetail(nomor, page = currentPage, limit = 100)
                if (response.status && response.data != null) {
                    if (surahDetail == null) surahDetail = response.data
                    allAyahs.addAll(response.data.ayahs)
                    
                    val total = response.pagination?.total ?: 0
                    if (allAyahs.size < total && response.data.ayahs.isNotEmpty()) {
                        currentPage++
                    } else {
                        break
                    }
                } else {
                    emit(Result.failure(Exception("Gagal mengambil detail surah $nomor")))
                    return@flow
                }
            } while (true)

            surahDetail?.let {
                val finalDetail = it.copy(ayahs = allAyahs)
                surahCache[nomor] = finalDetail
                emit(Result.success(finalDetail))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun getJuzDetail(nomorJuz: Int): Flow<Result<JuzDetail>> = flow {
        juzCache[nomorJuz]?.let {
            emit(Result.success(it))
            return@flow
        }

        try {
            Log.d(TAG, "Fetching Juz: $nomorJuz")
            var currentPage = 1
            val allAyahs = mutableListOf<Ayah>()
            
            do {
                val response = apiService.getJuz(nomorJuz, page = currentPage, limit = 100)
                if (response.status) {
                    allAyahs.addAll(response.data)
                    val total = response.pagination?.total ?: 0
                    if (allAyahs.size < total && response.data.isNotEmpty()) {
                        currentPage++
                    } else {
                        break
                    }
                } else {
                    emit(Result.failure(Exception("Gagal mengambil detail juz $nomorJuz")))
                    return@flow
                }
            } while (true)

            val juzDetail = JuzDetail(
                juz = nomorJuz,
                startSurahNama = allAyahs.firstOrNull()?.surahInfo?.nameLatin ?: "-",
                endSurahNama = allAyahs.lastOrNull()?.surahInfo?.nameLatin ?: "-",
                ayat = allAyahs
            )
            juzCache[nomorJuz] = juzDetail
            emit(Result.success(juzDetail))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Juz: ${e.message}")
            emit(Result.failure(e))
        }
    }

    override fun getSurahTafsir(nomorSurah: Int): Flow<Result<TafsirItem>> = flow {
        getSurahDetail(nomorSurah).collect { result ->
            result.onSuccess { surah ->
                val tafsirContent = surah.ayahs.map {
                    TafsirContent(ayat = it.ayahNumber, teks = it.tafsir?.kemenag?.long ?: "")
                }
                val tafsirItem = TafsirItem(
                    nomor = nomorSurah,
                    nama = surah.nameLatin,
                    tafsir = tafsirContent
                )
                emit(Result.success(tafsirItem))
            }.onFailure {
                emit(Result.failure(it))
            }
        }
    }

    override fun getAyatTafsir(nomorSurah: Int, nomorAyat: Int): Flow<Result<TafsirItem>> = flow {
        try {
            val response = apiService.getAyatDetail(nomorSurah, nomorAyat)
            if (response.status && response.data != null) {
                val ayah = response.data
                val tafsirItem = TafsirItem(
                    nomor = nomorSurah,
                    nama = ayah.surahInfo?.nameLatin ?: "",
                    tafsir = listOf(TafsirContent(ayat = nomorAyat, teks = ayah.tafsir?.kemenag?.long ?: ""))
                )
                emit(Result.success(tafsirItem))
            } else {
                emit(Result.failure(Exception("Gagal mengambil tafsir ayat $nomorAyat")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
