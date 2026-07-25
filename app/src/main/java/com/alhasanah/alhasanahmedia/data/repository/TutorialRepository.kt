package com.alhasanah.alhasanahmedia.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tutorialDataStore: DataStore<Preferences> by preferencesDataStore(name = "tutorial_prefs")

class TutorialRepository(private val context: Context) {

    companion object {
        private val KEY_TUTORIAL_COMPLETED = booleanPreferencesKey("wali_santri_tutorial_done")
        private val KEY_USER_TYPE = stringPreferencesKey("user_type_selection")
        private val KEY_TUTORIAL_PHASE2_COMPLETED = booleanPreferencesKey("wali_santri_tutorial_phase2_done")
    }

    val hasCompletedTutorial: Flow<Boolean> = context.tutorialDataStore.data
        .map { preferences ->
            preferences[KEY_TUTORIAL_COMPLETED] ?: false
        }

    val hasCompletedTutorialPhase2: Flow<Boolean> = context.tutorialDataStore.data
        .map { preferences ->
            preferences[KEY_TUTORIAL_PHASE2_COMPLETED] ?: false
        }

    val userType: Flow<String?> = context.tutorialDataStore.data
        .map { preferences ->
            preferences[KEY_USER_TYPE]
        }

    suspend fun setUserType(type: String) {
        context.tutorialDataStore.edit {
            it[KEY_USER_TYPE] = type
        }
    }

    suspend fun setTutorialCompleted() {
        context.tutorialDataStore.edit {
            it[KEY_TUTORIAL_COMPLETED] = true
        }
    }

    suspend fun setTutorialPhase2Completed() {
        context.tutorialDataStore.edit {
            it[KEY_TUTORIAL_PHASE2_COMPLETED] = true
        }
    }

    suspend fun resetTutorial() {
        context.tutorialDataStore.edit {
            it[KEY_TUTORIAL_COMPLETED] = false
            it[KEY_TUTORIAL_PHASE2_COMPLETED] = false
        }
    }
}
