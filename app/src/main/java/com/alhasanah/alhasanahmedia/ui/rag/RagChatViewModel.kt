package com.alhasanah.alhasanahmedia.ui.rag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.model.RagSource
import com.alhasanah.alhasanahmedia.data.model.WaliChild
import com.alhasanah.alhasanahmedia.data.repository.AuthRepository
import com.alhasanah.alhasanahmedia.data.repository.RagRepository
import com.alhasanah.alhasanahmedia.ui.auth.AuthenticationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class ChatRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val text: String,
    val sources: List<RagSource> = emptyList(),
    val showLoginAction: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class RagChatUiState(
    val input: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val children: List<WaliChild> = emptyList(),
    val selectedChildRef: String? = null,
    val remainingRequests: Int? = null
)

class RagChatViewModel(
    private val ragRepository: RagRepository,
    authRepository: AuthRepository
) : ViewModel() {

    val authenticationState: StateFlow<AuthenticationState> = authRepository.getAuthState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AuthenticationState.NotAuthenticated
        )

    private val _uiState = MutableStateFlow(RagChatUiState())
    val uiState: StateFlow<RagChatUiState> = _uiState.asStateFlow()

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value, errorMessage = null) }
    }

    fun onChildSelected(childRef: String) {
        _uiState.update { it.copy(selectedChildRef = childRef) }
    }

    fun sendMessage() {
        val snapshot = _uiState.value
        val query = snapshot.input.trim()
        if (query.isBlank() || snapshot.isLoading) return

        val isLoggedIn = authenticationState.value is AuthenticationState.Authenticated
        val userMessage = ChatMessage(role = ChatRole.USER, text = query)
        _uiState.update {
            it.copy(
                input = "",
                isLoading = true,
                errorMessage = null,
                messages = it.messages + userMessage
            )
        }

        if (!isLoggedIn && query.isPrivateSantriQuestion()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    messages = it.messages + ChatMessage(
                        role = ChatRole.ASSISTANT,
                        text = "anda tidak mempunyai akses pada informasi terkait, silahkan login",
                        showLoginAction = true
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            val result = if (isLoggedIn) {
                ragRepository.askWali(query, snapshot.selectedChildRef)
                    .map { response ->
                        RagAnswer(
                            text = response.answer,
                            sources = response.sources,
                            children = response.children,
                            selectedChildRef = response.selectedChildRef,
                            remainingRequests = response.remainingRequests,
                            hasRelevantContext = true,
                            isKitabQuestion = false
                        )
                    }
            } else if (query.isKitabQuestion()) {
                ragRepository.askPublicKitab(query)
                    .map { response ->
                        RagAnswer(
                            text = response.answer.ifBlank { "Maaf, referensi kitab untuk pertanyaan tersebut belum tersedia." },
                            sources = response.sources,
                            children = emptyList(),
                            selectedChildRef = null,
                            remainingRequests = response.remainingRequests,
                            hasRelevantContext = response.hasRelevantContext,
                            isKitabQuestion = true
                        )
                    }
            } else {
                ragRepository.askPublicPesantren(query)
                    .map { response ->
                        RagAnswer(
                            text = response.answer.ifBlank { "Maaf, Informasi tersebut belum tersedia." },
                            sources = response.sources,
                            children = emptyList(),
                            selectedChildRef = null,
                            remainingRequests = response.remainingRequests,
                            hasRelevantContext = response.hasRelevantContext,
                            isKitabQuestion = false
                        )
                    }
            }

            result.fold(
                onSuccess = { answer ->
                    val text = if (!answer.hasRelevantContext && !isLoggedIn) {
                        if (answer.isKitabQuestion) {
                            "Maaf, referensi kitab untuk pertanyaan tersebut belum tersedia."
                        } else {
                            "Maaf, informasi tersebut belum tersedia di sistem Al-Hasanah."
                        }
                    } else {
                        answer.text
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            children = answer.children.ifEmpty { it.children },
                            selectedChildRef = answer.selectedChildRef ?: it.selectedChildRef,
                            remainingRequests = answer.remainingRequests,
                            messages = it.messages + ChatMessage(
                                role = ChatRole.ASSISTANT,
                                text = text,
                                sources = answer.sources
                            )
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Maaf, belum dapat menjawab saat ini. Coba lagi nanti."
                        )
                    }
                }
            )
        }
    }

    private data class RagAnswer(
        val text: String,
        val sources: List<RagSource>,
        val children: List<WaliChild>,
        val selectedChildRef: String?,
        val remainingRequests: Int?,
        val hasRelevantContext: Boolean,
        val isKitabQuestion: Boolean
    )

    private fun String.isPrivateSantriQuestion(): Boolean {
        val text = lowercase()
        val keywords = listOf(
            "anak saya",
            "santri saya",
            "tagihan",
            "spp",
            "hafalan anak",
            "progres hafalan",
            "kedisiplinan",
            "pelanggaran",
            "perizinan",
            "izin anak",
            "kesehatan",
            "rekam medis",
            "prestasi anak",
            "status santri",
            "data santri",
            "nilai anak",
            "absensi"
        )
        return keywords.any { text.contains(it) }
    }

    private fun String.isKitabQuestion(): Boolean {
        val text = lowercase()
        val keywords = listOf(
            "kitab",
            "agama",
            "fiqih",
            "fikih",
            "hukum",
            "thaharah",
            "wudhu",
            "shalat",
            "sholat",
            "puasa",
            "zakat",
            "adab",
            "akhlak",
            "hadits",
            "hadis",
            "tafsir",
            "tauhid",
            "aqidah",
            "akidah"
        )
        return keywords.any { text.contains(it) }
    }
}
