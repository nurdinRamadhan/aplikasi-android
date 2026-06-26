package com.alhasanah.alhasanahmedia.ui.auth

import android.util.Log // Import Android Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.repository.AuthRepository
import com.alhasanah.alhasanahmedia.data.repository.NotificationRepository
import com.alhasanah.alhasanahmedia.data.repository.WaliSantriRepository
import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

// Sealed class for observing global authentication status
sealed class AuthenticationState {
    object NotAuthenticated : AuthenticationState()
    data class Authenticated(val userId: String) : AuthenticationState()
}

// Sealed class for handling the login screen's specific UI states
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val waliSantriRepository: WaliSantriRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val authenticationState: StateFlow<AuthenticationState> = authRepository.getAuthState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthenticationState.NotAuthenticated
        )

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _activeSantriNis = MutableStateFlow<String?>(null)
    val activeSantriNis: StateFlow<String?> = _activeSantriNis.asStateFlow()

    private val _currentUserRole = MutableStateFlow<String?>(null)
    val currentUserRole: StateFlow<String?> = _currentUserRole.asStateFlow()

    init {
        viewModelScope.launch {
            authenticationState.collect { state ->
                if (state is AuthenticationState.Authenticated) {
                    fetchAndSetCurrentRole()
                    fetchAndSetActiveSantri()
                    saveFcmToken(state.userId)
                } else {
                    _activeSantriNis.value = null
                    _currentUserRole.value = null
                }
            }
        }
    }

    private suspend fun fetchAndSetCurrentRole() {
        _currentUserRole.value = runCatching { authRepository.getCurrentUserRole()?.lowercase() }
            .getOrNull()
    }

    private suspend fun saveFcmToken(userId: String) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            notificationRepository.updateFCMToken(userId, token)
            Log.d("AuthViewModel", "FCM Token saved successfully for user $userId")
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error saving FCM Token", e)
        }
    }

    private suspend fun fetchAndSetActiveSantri() {
        try {
            val santriList = waliSantriRepository.getMySantriList()
            _activeSantriNis.value = santriList.firstOrNull()?.id
            Log.d("AuthViewModel", "Santri list size: ${santriList.size}")
            if (santriList.firstOrNull()?.id == null) {
                Log.w("AuthViewModel", "No active Santri NIS found for current wali")
            }
        } catch (e: Exception) {
            _activeSantriNis.value = null
            Log.e("AuthViewModel", "Error fetching santri list for current wali", e)
        }
    }

    fun getCurrentUser(): Flow<UserInfo?> {
        return authRepository.getCurrentUser()
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                authRepository.signIn(email, password)
                val sessionReady = withTimeoutOrNull(5_000) {
                    authRepository.getAuthState().first { it is AuthenticationState.Authenticated }
                }
                if (sessionReady is AuthenticationState.Authenticated) {
                    _loginState.value = LoginState.Success
                } else {
                    _loginState.value = LoginState.Error("Login berhasil, tetapi sesi belum siap. Silakan coba buka ulang aplikasi.")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.toLoginMessage())
                Log.e("AuthViewModel", "Login error: ${e.message}", e)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                runCatching { waliSantriRepository.clearSensitiveCache() }
                authRepository.signOut()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign out error: ${e.message}", e)
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }
}

private fun Throwable.toLoginMessage(): String {
    val raw = listOfNotNull(message, localizedMessage)
        .joinToString(" ")
        .lowercase()
    return when {
        raw.contains("invalid login credentials") ||
            raw.contains("invalid email or password") ||
            raw.contains("email not confirmed") -> "Login gagal. Email atau password salah, atau akun belum aktif."
        raw.contains("timeout") ||
            raw.contains("timed out") -> "Login gagal. Koneksi ke server terlalu lama. Coba lagi."
        raw.contains("certificate") ||
            raw.contains("ssl") ||
            raw.contains("handshake") ||
            raw.contains("trust anchor") -> "Login gagal. Koneksi aman ke server ditolak. Periksa konfigurasi sertifikat aplikasi."
        raw.contains("unable to resolve host") ||
            raw.contains("failed to connect") ||
            raw.contains("network") ||
            raw.contains("connection") -> "Login gagal. Periksa koneksi internet lalu coba lagi."
        else -> "Login gagal. ${localizedMessage ?: message ?: "Terjadi kesalahan saat login."}"
    }
}
