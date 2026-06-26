package com.alhasanah.alhasanahmedia.ui.santri

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.model.PublicPrestasiSantri
import com.alhasanah.alhasanahmedia.data.repository.PublicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PrestasiUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val items: List<PublicPrestasiSantri> = emptyList(),
    val categoryCounts: Map<String, Long> = emptyMap(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val hasMore: Boolean = false,
    val error: String? = null
)

class PrestasiViewModel(
    private val repository: PublicRepository
) : ViewModel() {
    private val pageSize = 50
    private val _uiState = MutableStateFlow(PrestasiUiState())
    val uiState: StateFlow<PrestasiUiState> = _uiState.asStateFlow()

    init {
        loadPrestasi()
    }

    fun loadPrestasi(
        kategori: String? = _uiState.value.selectedCategory,
        search: String = _uiState.value.searchQuery,
        reset: Boolean = true,
        refreshing: Boolean = false
    ) {
        viewModelScope.launch {
            val current = _uiState.value
            val offset = if (reset) 0 else current.items.size
            _uiState.value = current.copy(
                loading = true,
                refreshing = refreshing,
                selectedCategory = kategori,
                searchQuery = search,
                error = null
            )
            runCatching {
                val cleanSearch = search.trim().ifBlank { null }
                val page = repository.getPrestasiList(
                    kategori = kategori,
                    search = cleanSearch,
                    limit = pageSize + 1,
                    offset = offset
                )
                val counts = if (reset) {
                    repository.getPrestasiCategoryCounts(search = cleanSearch)
                        .associate { it.kategori to it.total }
                } else {
                    current.categoryCounts
                }
                page to counts
            }
                .onSuccess { (result, counts) ->
                    val page = result.take(pageSize)
                    _uiState.value = PrestasiUiState(
                        loading = false,
                        refreshing = false,
                        items = if (reset) page else current.items + page,
                        categoryCounts = counts,
                        selectedCategory = kategori,
                        searchQuery = search,
                        hasMore = result.size > pageSize
                    )
                }
                .onFailure {
                    _uiState.value = PrestasiUiState(
                        loading = false,
                        refreshing = false,
                        items = if (reset) emptyList() else current.items,
                        categoryCounts = current.categoryCounts,
                        selectedCategory = kategori,
                        searchQuery = search,
                        hasMore = if (reset) false else current.hasMore,
                        error = "Gagal memuat data prestasi."
                    )
                }
        }
    }

    fun selectCategory(kategori: String?) {
        loadPrestasi(kategori = kategori, reset = true)
    }

    fun search(query: String) {
        loadPrestasi(search = query, reset = true)
    }

    fun refresh() {
        loadPrestasi(reset = true, refreshing = true)
    }

    fun retry() {
        loadPrestasi(reset = true)
    }

    fun loadMore() {
        if (!_uiState.value.loading && _uiState.value.hasMore) {
            loadPrestasi(reset = false)
        }
    }
}
