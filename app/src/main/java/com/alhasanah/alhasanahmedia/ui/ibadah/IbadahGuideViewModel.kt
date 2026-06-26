package com.alhasanah.alhasanahmedia.ui.ibadah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.model.ibadah.IbadahGuide
import com.alhasanah.alhasanahmedia.data.model.ibadah.IbadahGuideCatalog
import com.alhasanah.alhasanahmedia.data.repository.IbadahGuideRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IbadahGuideUiState(
    val catalog: IbadahGuideCatalog = IbadahGuideCatalog(),
    val selectedCategory: String = "Semua",
    val selectedGuide: IbadahGuide? = null,
    val query: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val categories: List<String>
        get() = listOf("Semua") + catalog.guides.map { it.category }.distinct()

    val visibleGuides: List<IbadahGuide>
        get() {
            val byCategory = if (selectedCategory == "Semua") {
                catalog.guides
            } else {
                catalog.guides.filter { it.category == selectedCategory }
            }
            if (query.isBlank()) return byCategory
            return byCategory.filter { guide ->
                guide.title.contains(query, ignoreCase = true) ||
                    guide.category.contains(query, ignoreCase = true) ||
                    guide.summary.contains(query, ignoreCase = true) ||
                    guide.chapters.any { chapter ->
                        chapter.title.contains(query, ignoreCase = true) ||
                            chapter.description.contains(query, ignoreCase = true)
                    }
            }
        }
}

class IbadahGuideViewModel(
    private val repository: IbadahGuideRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(IbadahGuideUiState())
    val uiState: StateFlow<IbadahGuideUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.getCatalog() }
                .onSuccess { catalog ->
                    _uiState.update { it.copy(catalog = catalog, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Gagal memuat tuntunan ibadah")
                    }
                }
        }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category, selectedGuide = null) }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun selectGuide(guide: IbadahGuide) {
        _uiState.update { it.copy(selectedGuide = guide, query = "") }
    }

    fun clearSelectedGuide() {
        _uiState.update { it.copy(selectedGuide = null) }
    }
}
