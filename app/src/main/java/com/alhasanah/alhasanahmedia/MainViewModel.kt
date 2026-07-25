package com.alhasanah.alhasanahmedia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.repository.AuthRepository
import com.alhasanah.alhasanahmedia.data.repository.ThemeRepository
import com.alhasanah.alhasanahmedia.data.repository.TutorialRepository
import com.alhasanah.alhasanahmedia.ui.auth.AuthenticationState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val themeRepository: ThemeRepository,
    private val authRepository: AuthRepository,
    private val tutorialRepository: TutorialRepository
) : ViewModel() {

    val themeMode: StateFlow<Boolean?> = themeRepository.getThemeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val authenticationState: StateFlow<AuthenticationState> = authRepository.getAuthState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthenticationState.NotAuthenticated
        )

    // ── Tutorial State ─────────────────────────────────────────────────────
    val userType: StateFlow<String?> = tutorialRepository.userType
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val hasCompletedTutorial: StateFlow<Boolean> = tutorialRepository.hasCompletedTutorial
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val hasCompletedTutorialPhase2: StateFlow<Boolean> = tutorialRepository.hasCompletedTutorialPhase2
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun toggleTheme(isSystemDark: Boolean) {
        viewModelScope.launch {
            val currentVal = themeMode.value
            val newVal = if (currentVal != null) !currentVal else !isSystemDark
            themeRepository.setThemeMode(newVal)
        }
    }

    // ── Tutorial Methods ───────────────────────────────────────────────────
    fun setUserType(type: String) {
        viewModelScope.launch {
            tutorialRepository.setUserType(type)
        }
    }

    fun completeTutorial() {
        viewModelScope.launch {
            tutorialRepository.setTutorialCompleted()
        }
    }

    fun completeTutorialPhase2() {
        viewModelScope.launch {
            tutorialRepository.setTutorialPhase2Completed()
        }
    }

    fun resetTutorial() {
        viewModelScope.launch {
            tutorialRepository.resetTutorial()
        }
    }
}
