package com.alhasanah.alhasanahmedia.ui.alumni

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.model.AlumniDirectoryItem
import com.alhasanah.alhasanahmedia.data.repository.AlumniRepository
import com.alhasanah.alhasanahmedia.data.repository.AuthRepository
import com.alhasanah.alhasanahmedia.ui.auth.AuthenticationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class AlumniDirectoryUiState {
    object Loading : AlumniDirectoryUiState()
    object LoginRequired : AlumniDirectoryUiState()
    data class Ready(
        val alumni: List<AlumniDirectoryItem>,
        val searchQuery: String = "",
        val selectedYear: Int? = null,
        val selectedProvince: String? = null
    ) : AlumniDirectoryUiState()
    data class Error(val message: String) : AlumniDirectoryUiState()
}

class AlumniDirectoryViewModel(
    private val authRepository: AuthRepository,
    private val alumniRepository: AlumniRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlumniDirectoryUiState>(AlumniDirectoryUiState.Loading)
    val uiState: StateFlow<AlumniDirectoryUiState> = _uiState.asStateFlow()

    fun loadDirectory() {
        viewModelScope.launch {
            _uiState.value = AlumniDirectoryUiState.Loading
            val authState = authRepository.getAuthState().first()
            if (authState !is AuthenticationState.Authenticated) {
                _uiState.value = AlumniDirectoryUiState.LoginRequired
                return@launch
            }

            runCatching {
                alumniRepository.getAccess(authState.userId).also { access ->
                    require(access.canOpenForum) { "Direktori alumni hanya tersedia untuk alumni terverifikasi." }
                }
                alumniRepository.getAlumniDirectoryItems()
            }.onSuccess { alumni ->
                _uiState.value = AlumniDirectoryUiState.Ready(alumni = alumni)
            }.onFailure { error ->
                Log.e("AlumniDirectoryVM", "Gagal memuat direktori alumni", error)
                _uiState.value = AlumniDirectoryUiState.Error(
                    error.localizedMessage ?: "Direktori alumni belum dapat dimuat."
                )
            }
        }
    }

    fun setSearchQuery(value: String) {
        val current = _uiState.value
        if (current is AlumniDirectoryUiState.Ready) {
            _uiState.value = current.copy(searchQuery = value)
        }
    }

    fun setSelectedYear(value: Int?) {
        val current = _uiState.value
        if (current is AlumniDirectoryUiState.Ready) {
            _uiState.value = current.copy(selectedYear = value)
        }
    }

    fun setSelectedProvince(value: String?) {
        val current = _uiState.value
        if (current is AlumniDirectoryUiState.Ready) {
            _uiState.value = current.copy(selectedProvince = value)
        }
    }
}
