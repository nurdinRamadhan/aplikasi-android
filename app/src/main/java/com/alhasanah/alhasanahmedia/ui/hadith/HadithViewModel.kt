package com.alhasanah.alhasanahmedia.ui.hadith

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.model.hadith.HadithItem
import com.alhasanah.alhasanahmedia.data.model.hadith.HadithPaging
import com.alhasanah.alhasanahmedia.data.model.hadith.HadithSearchItem
import com.alhasanah.alhasanahmedia.data.repository.HadithRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HadithRangeGroup(
    val title: String,
    val subtitle: String,
    val startId: Int,
    val endId: Int,
    val page: Int
)

data class HadithTopicShortcut(
    val title: String,
    val keyword: String
)

data class HadithListUiState(
    val isLoading: Boolean = true,
    val hasSelectedGroup: Boolean = false,
    val selectedGroup: HadithRangeGroup? = null,
    val items: List<HadithItem> = emptyList(),
    val searchItems: List<HadithSearchItem> = emptyList(),
    val paging: HadithPaging = HadithPaging(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val isOfflineData: Boolean = false,
    val lastUpdatedAt: Long? = null,
    val cacheNotice: String? = null,
    val errorMessage: String? = null
) {
    val isSearchMode: Boolean
        get() = searchQuery.trim().length >= 4

    val rangeGroups: List<HadithRangeGroup>
        get() = buildList {
            repeat(12) { index ->
                val start = index * 100 + 1
                val end = start + 99
                add(
                    HadithRangeGroup(
                        title = "Hadis $start-$end",
                        subtitle = "Kelompok bacaan berdasarkan nomor hadis",
                        startId = start,
                        endId = end,
                        page = index * 10 + 1
                    )
                )
            }
        }

    val topicShortcuts: List<HadithTopicShortcut>
        get() = listOf(
            HadithTopicShortcut("Akhlak", "akhlak"),
            HadithTopicShortcut("Sholat", "sholat"),
            HadithTopicShortcut("Puasa", "puasa"),
            HadithTopicShortcut("Zakat", "zakat"),
            HadithTopicShortcut("Keluarga", "keluarga"),
            HadithTopicShortcut("Kiamat", "kiamat")
        )
}

data class HadithDetailUiState(
    val isLoading: Boolean = true,
    val hadith: HadithItem? = null,
    val isOfflineData: Boolean = false,
    val lastUpdatedAt: Long? = null,
    val cacheNotice: String? = null,
    val errorMessage: String? = null
)

class HadithViewModel(
    private val repository: HadithRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(HadithListUiState())
    val listState: StateFlow<HadithListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(HadithDetailUiState())
    val detailState: StateFlow<HadithDetailUiState> = _detailState.asStateFlow()

    private var searchJob: Job? = null

    fun loadExplore(page: Int = _listState.value.paging.current) {
        viewModelScope.launch {
            _listState.value = _listState.value.copy(
                isLoading = true,
                hasSelectedGroup = true,
                cacheNotice = null,
                errorMessage = null,
                isSearching = false
            )
            repository.exploreHadith(page = page, limit = 10).collect { result ->
                result.onSuccess { resource ->
                    val data = resource.data
                    _listState.value = _listState.value.copy(
                        isLoading = false,
                        items = data.hadith,
                        paging = data.paging,
                        isOfflineData = resource.isFromCache,
                        lastUpdatedAt = resource.updatedAt,
                        cacheNotice = resource.notice,
                        errorMessage = null
                    )
                }.onFailure { error ->
                    _listState.value = _listState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Gagal mengambil daftar hadis"
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _listState.value = _listState.value.copy(searchQuery = query)
        searchJob?.cancel()

        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _listState.value = _listState.value.copy(
                searchItems = emptyList(),
                isSearching = false,
                hasSelectedGroup = false,
                selectedGroup = null,
                cacheNotice = null,
                errorMessage = null
            )
            return
        }

        if (trimmed.length < 4) {
            _listState.value = _listState.value.copy(
                searchItems = emptyList(),
                isSearching = false,
                hasSelectedGroup = false,
                selectedGroup = null,
                cacheNotice = null,
                errorMessage = null
            )
            return
        }

        searchJob = viewModelScope.launch {
            delay(350)
            searchHadith(page = 1)
        }
    }

    fun searchHadith(page: Int = _listState.value.paging.current) {
        val keyword = _listState.value.searchQuery.trim()
        if (keyword.length < 4) return

        viewModelScope.launch {
            _listState.value = _listState.value.copy(
                isLoading = true,
                isSearching = true,
                hasSelectedGroup = true,
                selectedGroup = null,
                cacheNotice = null,
                errorMessage = null
            )
            repository.searchHadith(keyword = keyword, page = page, limit = 10).collect { result ->
                result.onSuccess { resource ->
                    val data = resource.data
                    _listState.value = _listState.value.copy(
                        isLoading = false,
                        searchItems = data.hadith,
                        paging = data.paging,
                        isOfflineData = resource.isFromCache,
                        lastUpdatedAt = resource.updatedAt,
                        cacheNotice = resource.notice,
                        errorMessage = null
                    )
                }.onFailure { error ->
                    _listState.value = _listState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Gagal mencari hadis"
                    )
                }
            }
        }
    }

    fun loadPage(page: Int) {
        if (_listState.value.isSearchMode) {
            searchHadith(page)
        } else {
            loadExplore(page)
        }
    }

    fun openRangeGroup(group: HadithRangeGroup) {
        _listState.value = _listState.value.copy(
            selectedGroup = group,
            searchQuery = "",
            searchItems = emptyList()
        )
        loadExplore(page = group.page)
    }

    fun openTopic(topic: HadithTopicShortcut) {
        _listState.value = _listState.value.copy(
            searchQuery = topic.keyword,
            selectedGroup = null
        )
        searchHadith(page = 1)
    }

    fun backToGroups() {
        searchJob?.cancel()
        _listState.value = _listState.value.copy(
            isLoading = false,
            hasSelectedGroup = false,
            selectedGroup = null,
            items = emptyList(),
            searchItems = emptyList(),
            paging = HadithPaging(),
            searchQuery = "",
            isSearching = false,
            cacheNotice = null,
            errorMessage = null
        )
    }

    fun loadDetail(id: Int) {
        viewModelScope.launch {
            _detailState.value = HadithDetailUiState(isLoading = true)
            repository.getHadithDetail(id).collect { result ->
                result.onSuccess { resource ->
                    _detailState.value = HadithDetailUiState(
                        isLoading = false,
                        hadith = resource.data,
                        isOfflineData = resource.isFromCache,
                        lastUpdatedAt = resource.updatedAt,
                        cacheNotice = resource.notice
                    )
                }.onFailure { error ->
                    _detailState.value = HadithDetailUiState(
                        isLoading = false,
                        errorMessage = error.message ?: "Hadis tidak ditemukan"
                    )
                }
            }
        }
    }

    fun loadNext() {
        val current = _detailState.value.hadith ?: return
        loadAdjacent(current.id, next = true)
    }

    fun loadPrevious() {
        val current = _detailState.value.hadith ?: return
        loadAdjacent(current.id, next = false)
    }

    private fun loadAdjacent(id: Int, next: Boolean) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isLoading = true, cacheNotice = null, errorMessage = null)
            val flow = if (next) repository.getNextHadith(id) else repository.getPreviousHadith(id)
            flow.collect { result ->
                result.onSuccess { resource ->
                    _detailState.value = HadithDetailUiState(
                        isLoading = false,
                        hadith = resource.data,
                        isOfflineData = resource.isFromCache,
                        lastUpdatedAt = resource.updatedAt,
                        cacheNotice = resource.notice
                    )
                }.onFailure { error ->
                    _detailState.value = _detailState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Hadis tidak tersedia"
                    )
                }
            }
        }
    }
}
