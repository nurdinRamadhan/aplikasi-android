package com.alhasanah.alhasanahmedia.ui.quran

import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.model.quran.Ayah
import com.alhasanah.alhasanahmedia.data.model.quran.JuzDetail
import com.alhasanah.alhasanahmedia.data.model.quran.TafsirItem
import com.alhasanah.alhasanahmedia.data.model.quran.TafsirContent
import com.alhasanah.alhasanahmedia.data.repository.QuranBookmarkRepository
import com.alhasanah.alhasanahmedia.data.repository.QuranRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JuzDetailViewModel(
    private val repository: QuranRepository,
    private val bookmarkRepository: QuranBookmarkRepository,
    private val juzNomor: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuranUiState<JuzDetail>>(QuranUiState.Loading)
    val uiState: StateFlow<QuranUiState<JuzDetail>> = _uiState.asStateFlow()

    private val _tafsirState = MutableStateFlow<QuranUiState<TafsirItem>?>(null)
    val tafsirState: StateFlow<QuranUiState<TafsirItem>?> = _tafsirState.asStateFlow()

    private val _currentPlayingAyat = MutableStateFlow<String?>(null) // Format: "surah:ayat" or "streaming"
    val currentPlayingAyat: StateFlow<String?> = _currentPlayingAyat.asStateFlow()

    val bookmarks: StateFlow<Set<String>> = bookmarkRepository.getBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private var mediaPlayer: MediaPlayer? = null
    private var currentStreamingIndex = -1

    init {
        fetchJuzDetail()
    }

    fun playFullJuz() {
        val state = _uiState.value
        if (state !is QuranUiState.Success) return

        if (_currentPlayingAyat.value == "streaming") {
            stopAudio()
            return
        }

        stopAudio()
        currentStreamingIndex = 0
        _currentPlayingAyat.value = "streaming"
        startStreaming(state.data)
    }

    private fun startStreaming(juz: JuzDetail) {
        if (currentStreamingIndex >= juz.ayat.size) {
            stopAudio()
            return
        }

        val ayah = juz.ayat[currentStreamingIndex]
        val url = ayah.audioUrl ?: ""

        if (url.isEmpty()) {
            currentStreamingIndex++
            startStreaming(juz)
            return
        }

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener { start() }
                setOnCompletionListener {
                    currentStreamingIndex++
                    startStreaming(juz)
                }
                setOnErrorListener { _, _, _ ->
                    currentStreamingIndex++
                    startStreaming(juz)
                    true
                }
            } catch (e: Exception) {
                currentStreamingIndex++
                startStreaming(juz)
            }
        }
    }

    fun playAudio(url: String, surahNomor: Int, ayatNomor: Int) {
        if (url.isEmpty()) return
        val ayatId = "$surahNomor:$ayatNomor"
        if (_currentPlayingAyat.value == ayatId) {
            stopAudio()
            return
        }

        stopAudio()
        _currentPlayingAyat.value = ayatId
        
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener { start() }
                setOnCompletionListener { _currentPlayingAyat.value = null }
                setOnErrorListener { _, _, _ ->
                    _currentPlayingAyat.value = null
                    true
                }
            } catch (e: Exception) {
                _currentPlayingAyat.value = null
            }
        }
    }

    fun stopAudio() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        _currentPlayingAyat.value = null
        currentStreamingIndex = -1
    }

    fun fetchJuzDetail() = viewModelScope.launch {
        _uiState.value = QuranUiState.Loading
        repository.getJuzDetail(juzNomor).collect { result ->
            result.onSuccess { _uiState.value = QuranUiState.Success(it) }
            .onFailure { _uiState.value = QuranUiState.Error(it.message ?: "Gagal memuat Juz") }
        }
    }

    fun fetchTafsir(surahNomor: Int, ayatNomor: Int) = viewModelScope.launch {
        _tafsirState.value = QuranUiState.Loading
        val currentJuz = (_uiState.value as? QuranUiState.Success)?.data
        if (currentJuz != null) {
            val ayah = currentJuz.ayat.find { it.surahNumber == surahNomor && it.ayahNumber == ayatNomor }
            if (ayah != null && ayah.tafsir?.kemenag?.long != null) {
                _tafsirState.value = QuranUiState.Success(TafsirItem(surahNomor, ayah.surahInfo?.nameLatin ?: "Surah $surahNomor", 
                    listOf(TafsirContent(ayatNomor, ayah.tafsir.kemenag.long))))
                return@launch
            }
        }
        repository.getAyatTafsir(surahNomor, ayatNomor).collect { result ->
            result.onSuccess { _tafsirState.value = QuranUiState.Success(it) }
            .onFailure { _tafsirState.value = QuranUiState.Error(it.message ?: "Gagal memuat tafsir") }
        }
    }

    fun clearTafsir() { _tafsirState.value = null }
    fun toggleBookmark(surahNomor: Int, ayatNomor: Int) = viewModelScope.launch { bookmarkRepository.toggleBookmark(surahNomor, ayatNomor) }
    override fun onCleared() { super.onCleared(); stopAudio() }
}
