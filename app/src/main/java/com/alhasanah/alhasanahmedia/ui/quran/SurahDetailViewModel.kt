package com.alhasanah.alhasanahmedia.ui.quran

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.model.quran.Ayah
import com.alhasanah.alhasanahmedia.data.model.quran.QuranQori
import com.alhasanah.alhasanahmedia.data.model.quran.QuranQoriCatalog
import com.alhasanah.alhasanahmedia.data.model.quran.SurahDetail
import com.alhasanah.alhasanahmedia.data.repository.QuranRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.alhasanah.alhasanahmedia.data.model.quran.TafsirItem
import com.alhasanah.alhasanahmedia.data.model.quran.TafsirContent
import com.alhasanah.alhasanahmedia.data.repository.QuranBookmarkRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SurahDetailViewModel(
    private val repository: QuranRepository,
    private val bookmarkRepository: QuranBookmarkRepository,
    private val surahNomor: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuranUiState<SurahDetail>>(QuranUiState.Loading)
    val uiState: StateFlow<QuranUiState<SurahDetail>> = _uiState.asStateFlow()

    private val _tafsirState = MutableStateFlow<QuranUiState<TafsirItem>?>(null)
    val tafsirState: StateFlow<QuranUiState<TafsirItem>?> = _tafsirState.asStateFlow()

    private val _currentPlayingAyat = MutableStateFlow<Int?>(null) // null = idle, 0 = streaming full, >0 = per ayat
    val currentPlayingAyat: StateFlow<Int?> = _currentPlayingAyat.asStateFlow()

    val bookmarks: StateFlow<Set<String>> = bookmarkRepository.getBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val selectedQori: StateFlow<QuranQori> = bookmarkRepository.selectedQori
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuranQoriCatalog.defaultQori)

    private var mediaPlayer: MediaPlayer? = null

    init {
        fetchSurahDetail()
    }

    fun playFullSurah() {
        val state = _uiState.value
        if (state !is QuranUiState.Success) return

        if (_currentPlayingAyat.value == 0) {
            stopAudio()
            return
        }

        val qoriId = selectedQori.value.id
        val url = state.data.audioUrlFor(qoriId) ?: return
        if (url.isEmpty()) return
        playAudio(url, 0)
    }

    fun playAyah(ayah: Ayah) {
        val url = ayah.audioUrlFor(selectedQori.value.id) ?: return
        playAudio(url, ayah.ayahNumber)
    }

    fun selectQori(qoriId: String) = viewModelScope.launch {
        if (selectedQori.value.id == qoriId) return@launch
        stopAudio()
        bookmarkRepository.setSelectedQori(qoriId)
    }

    fun playAudio(url: String, ayatNomor: Int) {
        if (url.isEmpty()) return
        if (_currentPlayingAyat.value == ayatNomor) {
            stopAudio()
            return
        }

        stopAudio()
        _currentPlayingAyat.value = ayatNomor
        mediaPlayer = buildMediaPlayer(
            url = url,
            onPrepared = { it.start() },
            onCompleted = {
                releaseMediaPlayer()
                _currentPlayingAyat.value = null
            },
            onError = {
                releaseMediaPlayer()
                _currentPlayingAyat.value = null
            }
        )
    }

    fun stopAudio() {
        releaseMediaPlayer()
        _currentPlayingAyat.value = null
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    private fun buildMediaPlayer(
        url: String,
        onPrepared: (MediaPlayer) -> Unit,
        onCompleted: () -> Unit,
        onError: () -> Unit
    ): MediaPlayer? {
        return try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { onPrepared(it) }
                setOnCompletionListener { onCompleted() }
                setOnErrorListener { _, _, _ ->
                    onError()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            onError()
            null
        }
    }

    fun fetchSurahDetail() = viewModelScope.launch {
        _uiState.value = QuranUiState.Loading
        repository.getSurahDetail(surahNomor).collect { result ->
            result.onSuccess { _uiState.value = QuranUiState.Success(it) }
            .onFailure { _uiState.value = QuranUiState.Error(it.message ?: "Gagal memuat surah") }
        }
    }

    fun fetchTafsir(ayatNomor: Int) = viewModelScope.launch {
        _tafsirState.value = QuranUiState.Loading
        val currentSurah = (_uiState.value as? QuranUiState.Success)?.data
        if (currentSurah != null) {
            val ayah = currentSurah.ayahs.find { it.ayahNumber == ayatNomor }
            if (ayah != null && ayah.tafsir?.kemenag?.long != null) {
                _tafsirState.value = QuranUiState.Success(TafsirItem(surahNomor, currentSurah.nameLatin, 
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
    fun toggleBookmark(ayatNomor: Int) = viewModelScope.launch { bookmarkRepository.toggleBookmark(surahNomor, ayatNomor) }
    override fun onCleared() { super.onCleared(); stopAudio() }
}
