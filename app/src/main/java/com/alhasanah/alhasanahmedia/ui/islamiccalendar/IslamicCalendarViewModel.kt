package com.alhasanah.alhasanahmedia.ui.islamiccalendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.repository.IslamicCalendarBundle
import com.alhasanah.alhasanahmedia.data.repository.IslamicCalendarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IslamicCalendarUiState(
    val isLoading: Boolean = true,
    val bundle: IslamicCalendarBundle? = null,
    val isOfflineData: Boolean = false,
    val notice: String? = null,
    val errorMessage: String? = null
)

class IslamicCalendarViewModel(
    private val repository: IslamicCalendarRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(IslamicCalendarUiState())
    val uiState: StateFlow<IslamicCalendarUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, notice = null, errorMessage = null)
            repository.getCalendarBundle().collect { result ->
                result.onSuccess { resource ->
                    _uiState.value = IslamicCalendarUiState(
                        isLoading = false,
                        bundle = resource.data,
                        isOfflineData = resource.isFromCache,
                        notice = resource.notice
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Kalender tidak tersedia"
                    )
                }
            }
        }
    }
}
