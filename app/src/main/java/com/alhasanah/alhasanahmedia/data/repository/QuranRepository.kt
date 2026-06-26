package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.quran.JuzDetail
import com.alhasanah.alhasanahmedia.data.model.quran.SurahDetail
import com.alhasanah.alhasanahmedia.data.model.quran.SurahListItem
import com.alhasanah.alhasanahmedia.data.model.quran.TafsirItem
import kotlinx.coroutines.flow.Flow

interface QuranRepository {
    fun getSurahList(): Flow<Result<List<SurahListItem>>>
    fun getSurahDetail(nomor: Int): Flow<Result<SurahDetail>>
    fun getJuzDetail(nomorJuz: Int): Flow<Result<JuzDetail>>
    fun getSurahTafsir(nomorSurah: Int): Flow<Result<TafsirItem>>
    fun getAyatTafsir(nomorSurah: Int, nomorAyat: Int): Flow<Result<TafsirItem>>
}

