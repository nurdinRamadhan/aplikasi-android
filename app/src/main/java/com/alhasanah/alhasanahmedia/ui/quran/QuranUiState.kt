package com.alhasanah.alhasanahmedia.ui.quran

sealed class QuranUiState<out T> {
    object Loading : QuranUiState<Nothing>()
    data class Success<T>(val data: T) : QuranUiState<T>()
    data class Error(val message: String) : QuranUiState<Nothing>()
}
