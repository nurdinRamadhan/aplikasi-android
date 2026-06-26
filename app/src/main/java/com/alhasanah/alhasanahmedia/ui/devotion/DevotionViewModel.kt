package com.alhasanah.alhasanahmedia.ui.devotion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.model.devotion.DevotionItem
import com.alhasanah.alhasanahmedia.data.model.devotion.DevotionLibraryData
import com.alhasanah.alhasanahmedia.data.model.devotion.KitabBook
import com.alhasanah.alhasanahmedia.data.model.devotion.KitabChapter
import com.alhasanah.alhasanahmedia.data.repository.DevotionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DevotionTab(val label: String) {
    DOA("Doa"),
    DZIKIR("Dzikir"),
    KITAB("Kitab")
}

data class DevotionUiState(
    val selectedTab: DevotionTab = DevotionTab.DOA,
    val selectedCategory: String = "Semua",
    val query: String = "",
    val library: DevotionLibraryData = DevotionLibraryData(),
    val selectedBook: KitabBook? = null,
    val chapters: List<KitabChapter> = emptyList(),
    val selectedChapter: KitabChapter? = null,
    val isLoading: Boolean = true,
    val isChapterLoading: Boolean = false,
    val isFromCache: Boolean = false,
    val notice: String? = null,
    val error: String? = null
) {
    val devotionCategories: List<String>
        get() = listOf("Semua") + visibleDevotions.map { it.category }.distinct()

    val visibleDevotions: List<DevotionItem>
        get() {
            val base = when (selectedTab) {
                DevotionTab.DOA -> library.devotions.filterNot { it.category.contains("dzikir", ignoreCase = true) }
                DevotionTab.DZIKIR -> library.devotions.filter { it.category.contains("dzikir", ignoreCase = true) }
                DevotionTab.KITAB -> emptyList()
            }
            val byCategory = if (selectedCategory == "Semua") base else base.filter { it.category == selectedCategory }
            if (query.isBlank()) return byCategory
            return byCategory.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.translation.contains(query, ignoreCase = true) ||
                    it.latin.contains(query, ignoreCase = true)
            }
        }

    val visibleBooks: List<KitabBook>
        get() {
            val books = library.kitabBooks
            if (query.isBlank()) return books
            return books.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.author.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true)
            }
        }

    val visibleChapters: List<KitabChapter>
        get() {
            if (query.isBlank()) return chapters
            return chapters.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.content.contains(query, ignoreCase = true) ||
                    it.translation.contains(query, ignoreCase = true)
            }
        }
}

class DevotionViewModel(
    private val repository: DevotionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DevotionUiState())
    val uiState: StateFlow<DevotionUiState> = _uiState.asStateFlow()
    private var chapterJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, notice = null) }
            repository.getLibrary().collect { result ->
                result
                    .onSuccess { resource ->
                        _uiState.update {
                            it.copy(
                                library = resource.data,
                                isLoading = false,
                                isFromCache = resource.isFromCache,
                                notice = resource.notice,
                                error = null
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(isLoading = false, error = error.message ?: "Gagal memuat pustaka")
                        }
                    }
            }
        }
    }

    fun selectTab(tab: DevotionTab) {
        _uiState.update {
            it.copy(
                selectedTab = tab,
                selectedCategory = "Semua",
                query = "",
                selectedBook = if (tab == DevotionTab.KITAB) it.selectedBook else null,
                selectedChapter = if (tab == DevotionTab.KITAB) it.selectedChapter else null
            )
        }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun clearSelectedBook() {
        chapterJob?.cancel()
        _uiState.update { it.copy(selectedBook = null, selectedChapter = null, chapters = emptyList(), query = "") }
    }

    fun selectBook(book: KitabBook) {
        chapterJob?.cancel()
        chapterJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedBook = book,
                    selectedChapter = null,
                    chapters = emptyList(),
                    isChapterLoading = true,
                    error = null,
                    query = ""
                )
            }
            repository.getKitabChapters(book.slug).collect { result ->
                result
                    .onSuccess { resource ->
                        _uiState.update {
                            it.copy(
                                chapters = resource.data,
                                isChapterLoading = false,
                                isFromCache = resource.isFromCache,
                                notice = resource.notice
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(isChapterLoading = false, error = error.message ?: "Gagal memuat bab kitab")
                        }
                    }
            }
        }
    }

    fun selectChapter(chapter: KitabChapter) {
        _uiState.update { it.copy(selectedChapter = chapter) }
        viewModelScope.launch {
            repository.getKitabChapterDetail(chapter.bookSlug, chapter.number).collect { result ->
                result.onSuccess { resource ->
                    _uiState.update { it.copy(selectedChapter = resource.data, notice = resource.notice) }
                }
            }
        }
    }
}
