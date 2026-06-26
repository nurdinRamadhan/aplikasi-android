package com.alhasanah.alhasanahmedia.ui.alumni

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.model.AlumniFollowUser
import com.alhasanah.alhasanahmedia.data.model.AlumniProfile
import com.alhasanah.alhasanahmedia.data.model.UpdateAlumniProfileDto
import com.alhasanah.alhasanahmedia.data.repository.AlumniRepository
import com.alhasanah.alhasanahmedia.data.repository.AuthRepository
import com.alhasanah.alhasanahmedia.data.repository.ForumRepository
import com.alhasanah.alhasanahmedia.ui.auth.AuthenticationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class AlumniProfileUiState {
    object Loading : AlumniProfileUiState()
    object LoginRequired : AlumniProfileUiState()
    data class Ready(
        val viewerId: String,
        val profile: AlumniProfile,
        val isSaving: Boolean = false,
        val message: String? = null
    ) : AlumniProfileUiState()
    data class Error(val message: String) : AlumniProfileUiState()
}

enum class FollowListTab {
    FOLLOWERS, FOLLOWING
}

class AlumniProfileViewModel(
    private val authRepository: AuthRepository,
    private val alumniRepository: AlumniRepository,
    private val forumRepository: ForumRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlumniProfileUiState>(AlumniProfileUiState.Loading)
    val uiState: StateFlow<AlumniProfileUiState> = _uiState.asStateFlow()

    private val _followListState = MutableStateFlow(FollowListUiState())
    val followListState: StateFlow<FollowListUiState> = _followListState.asStateFlow()

    fun loadProfile(targetId: String? = null) {
        viewModelScope.launch {
            _uiState.value = AlumniProfileUiState.Loading
            val authState = authRepository.getAuthState().first()
            val viewerId = (authState as? AuthenticationState.Authenticated)?.userId
            if (viewerId.isNullOrBlank()) {
                _uiState.value = AlumniProfileUiState.LoginRequired
                return@launch
            }

            val alumniId = targetId?.takeIf { it.isNotBlank() } ?: viewerId
            alumniRepository.getCachedProfile(viewerId, alumniId)?.let { cached ->
                _uiState.value = AlumniProfileUiState.Ready(
                    viewerId = viewerId,
                    profile = cached
                )
            }
            runCatching {
                alumniRepository.getProfile(viewerId, alumniId)
            }.onSuccess { profile ->
                _uiState.value = AlumniProfileUiState.Ready(viewerId, profile)
            }.onFailure { error ->
                Log.e("AlumniProfileVM", "Gagal memuat profil alumni", error)
                _uiState.value = AlumniProfileUiState.Error(
                    error.localizedMessage ?: "Profil alumni belum dapat dimuat."
                )
            }
        }
    }

    fun saveProfile(update: UpdateAlumniProfileDto, avatarUri: Uri? = null) {
        val current = _uiState.value as? AlumniProfileUiState.Ready ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isSaving = true, message = null)
            runCatching {
                val avatarPath = avatarUri?.let { alumniRepository.uploadAvatar(current.viewerId, it) }
                val saved = alumniRepository.updateProfile(
                    userId = current.viewerId,
                    update = update.copy(
                        avatarStoragePath = avatarPath ?: update.avatarStoragePath
                    )
                )
                alumniRepository.getProfile(current.viewerId, saved.id)
            }.onSuccess { profile ->
                _uiState.value = AlumniProfileUiState.Ready(
                    viewerId = current.viewerId,
                    profile = profile,
                    message = "Profil alumni diperbarui."
                )
            }.onFailure { error ->
                Log.e("AlumniProfileVM", "Gagal menyimpan profil alumni", error)
                _uiState.value = current.copy(
                    isSaving = false,
                    message = error.localizedMessage ?: "Gagal menyimpan profil."
                )
            }
        }
    }

    fun saveSettings(
        showProfession: Boolean,
        showLocation: Boolean,
        showWhatsapp: Boolean,
        forumNotifyReplies: Boolean,
        forumNotifyReactions: Boolean
    ) {
        val current = _uiState.value as? AlumniProfileUiState.Ready ?: return
        val alumni = current.profile.alumni
        saveProfile(
            UpdateAlumniProfileDto(
                fullName = alumni.fullName,
                noWa = alumni.noWa,
                profesiSekarang = alumni.profesiSekarang,
                instansiKerja = alumni.instansiKerja,
                alamatDomisili = alumni.alamatDomisili,
                provinceCode = alumni.provinceCode,
                provinceName = alumni.provinceName,
                regencyCode = alumni.regencyCode,
                regencyName = alumni.regencyName,
                districtCode = alumni.districtCode,
                districtName = alumni.districtName,
                villageCode = alumni.villageCode,
                villageName = alumni.villageName,
                postalCode = alumni.postalCode,
                addressDetail = alumni.addressDetail,
                bio = alumni.bio,
                avatarStoragePath = alumni.avatarStoragePath,
                showWhatsapp = showWhatsapp,
                showProfession = showProfession,
                showLocation = showLocation,
                forumNotifyReplies = forumNotifyReplies,
                forumNotifyReactions = forumNotifyReactions
            )
        )
    }

    fun repostThread(sourceThreadId: String, content: String) {
        val current = _uiState.value as? AlumniProfileUiState.Ready ?: return
        val cleanContent = content.trim()
        if (cleanContent.isBlank()) return

        viewModelScope.launch {
            _uiState.value = current.copy(isSaving = true, message = null)
            runCatching {
                forumRepository.createRepost(
                    authorId = current.viewerId,
                    sourceThreadId = sourceThreadId,
                    content = cleanContent
                )
                alumniRepository.getProfile(current.viewerId, current.profile.alumni.id)
            }.onSuccess { profile ->
                _uiState.value = AlumniProfileUiState.Ready(
                    viewerId = current.viewerId,
                    profile = profile,
                    message = "Postingan dibagikan ulang."
                )
            }.onFailure { error ->
                Log.e("AlumniProfileVM", "Gagal membagikan ulang postingan", error)
                _uiState.value = current.copy(
                    isSaving = false,
                    message = error.localizedMessage ?: "Gagal membagikan ulang postingan."
                )
            }
        }
    }

    fun toggleFollow() {
        val current = _uiState.value as? AlumniProfileUiState.Ready ?: return
        val alumniId = current.profile.alumni.id
        if (current.profile.isOwnProfile || alumniId == current.viewerId) return

        viewModelScope.launch {
            _uiState.value = current.copy(isSaving = true, message = null)
            val isFollowing = current.profile.followStats.followedByMe
            runCatching {
                if (isFollowing) {
                    alumniRepository.unfollow(current.viewerId, alumniId)
                } else {
                    alumniRepository.follow(current.viewerId, alumniId)
                }
                alumniRepository.getProfile(current.viewerId, alumniId)
            }.onSuccess { profile ->
                _uiState.value = AlumniProfileUiState.Ready(
                    viewerId = current.viewerId,
                    profile = profile,
                    message = if (isFollowing) "Berhenti mengikuti alumni." else "Alumni diikuti."
                )
            }.onFailure { error ->
                Log.e("AlumniProfileVM", "Gagal memperbarui follow alumni", error)
                _uiState.value = current.copy(
                    isSaving = false,
                    message = error.localizedMessage ?: "Gagal memperbarui status follow."
                )
            }
        }
    }

    fun loadFollowList(initialTab: FollowListTab) {
        val current = _uiState.value as? AlumniProfileUiState.Ready ?: return
        val alumniId = current.profile.alumni.id
        viewModelScope.launch {
            _followListState.value = FollowListUiState(
                visible = true,
                selectedTab = initialTab,
                isLoading = true
            )
            runCatching {
                val followers = alumniRepository.getFollowers(current.viewerId, alumniId)
                val following = alumniRepository.getFollowing(current.viewerId, alumniId)
                followers to following
            }.onSuccess { (followers, following) ->
                _followListState.value = FollowListUiState(
                    visible = true,
                    selectedTab = initialTab,
                    followers = followers,
                    following = following
                )
            }.onFailure { error ->
                Log.e("AlumniProfileVM", "Gagal memuat followers alumni", error)
                _followListState.value = FollowListUiState(
                    visible = true,
                    selectedTab = initialTab,
                    errorMessage = error.localizedMessage ?: "Gagal memuat daftar follow."
                )
            }
        }
    }

    fun selectFollowListTab(tab: FollowListTab) {
        _followListState.value = _followListState.value.copy(selectedTab = tab)
    }

    fun closeFollowList() {
        _followListState.value = FollowListUiState()
    }

    fun clearMessage() {
        val current = _uiState.value
        if (current is AlumniProfileUiState.Ready) {
            _uiState.value = current.copy(message = null)
        }
    }
}

data class FollowListUiState(
    val visible: Boolean = false,
    val selectedTab: FollowListTab = FollowListTab.FOLLOWERS,
    val followers: List<AlumniFollowUser> = emptyList(),
    val following: List<AlumniFollowUser> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
