package com.alhasanah.alhasanahmedia.ui.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.model.quran.SurahListItem
import com.alhasanah.alhasanahmedia.data.repository.QuranRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.alhasanah.alhasanahmedia.data.model.quran.Ayah
import com.alhasanah.alhasanahmedia.data.repository.QuranBookmarkRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class QuranViewModel(
    private val repository: QuranRepository,
    private val bookmarkRepository: QuranBookmarkRepository
) : ViewModel() {

    private val _surahListState = MutableStateFlow<QuranUiState<List<SurahListItem>>>(QuranUiState.Loading)
    val surahListState: StateFlow<QuranUiState<List<SurahListItem>>> = _surahListState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val bookmarks = bookmarkRepository.getBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _bookmarkedAyatDetails = MutableStateFlow<Map<String, Ayah>>(emptyMap())
    val bookmarkedAyatDetails: StateFlow<Map<String, Ayah>> = _bookmarkedAyatDetails.asStateFlow()

    // Filtered list based on search query
    val filteredSurahList = combine(_surahListState, _searchQuery) { state, query ->
        if (state is QuranUiState.Success) {
            if (query.isEmpty()) {
                state
            } else {
                val filtered = state.data.filter {
                    it.nameLatin.contains(query, ignoreCase = true) ||
                            it.arti.contains(query, ignoreCase = true)
                }
                QuranUiState.Success(filtered)
            }
        } else {
            state
        }
    }

    init {
        fetchSurahList()
        observeBookmarks()
    }

    private fun observeBookmarks() {
        viewModelScope.launch {
            bookmarks.collect { bookmarkIds ->
                bookmarkIds.forEach { id ->
                    if (!_bookmarkedAyatDetails.value.containsKey(id)) {
                        fetchAyatDetail(id)
                    }
                }
                // Cleanup removed bookmarks
                val currentDetails = _bookmarkedAyatDetails.value.toMutableMap()
                val removedIds = currentDetails.keys.filter { !bookmarkIds.contains(it) }
                if (removedIds.isNotEmpty()) {
                    removedIds.forEach { currentDetails.remove(it) }
                    _bookmarkedAyatDetails.value = currentDetails
                }
            }
        }
    }

    private fun fetchAyatDetail(id: String) {
        val parts = id.split(":")
        if (parts.size != 2) return
        val surahNo = parts[0].toIntOrNull() ?: return
        val ayatNo = parts[1].toIntOrNull() ?: return

        viewModelScope.launch {
            repository.getSurahDetail(surahNo).collect { result ->
                result.onSuccess { surah ->
                    val ayat = surah.ayahs.find { it.ayahNumber == ayatNo }
                    if (ayat != null) {
                        val current = _bookmarkedAyatDetails.value.toMutableMap()
                        current[id] = ayat
                        _bookmarkedAyatDetails.value = current
                    }
                }
            }
        }
    }

    fun removeBookmark(surahNo: Int, ayatNo: Int) {
        viewModelScope.launch {
            bookmarkRepository.toggleBookmark(surahNo, ayatNo)
        }
    }

    fun fetchSurahList() {
        viewModelScope.launch {
            _surahListState.value = QuranUiState.Loading
            repository.getSurahList().collect { result ->
                result.onSuccess {
                    _surahListState.value = QuranUiState.Success(it)
                }.onFailure {
                    _surahListState.value = QuranUiState.Error(it.message ?: "Terjadi kesalahan")
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
}
