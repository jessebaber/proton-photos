/*
 * Photos for Proton
 * Copyright (C) 2026 Akoos <https://akoos.eu>
 *
 * Source:  https://github.com/gitakoos/proton-photos
 * Website: https://www.photosforproton.eu
 *
 * This file is part of Photos for Proton.
 *
 * Photos for Proton is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package eu.akoos.photos.presentation.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.akoos.photos.domain.catalog.PeopleCatalog
import eu.akoos.photos.domain.entity.LOCAL_PARTITION
import eu.akoos.photos.domain.ml.FaceEdits
import eu.akoos.photos.domain.ml.FaceIndexing
import eu.akoos.photos.domain.settings.Settings
import eu.akoos.photos.domain.settings.SettingsStore
import eu.akoos.photos.presentation.gallery.FaceBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.domain.AccountManager
import javax.inject.Inject

/** One excluded face reduced to what a tile needs: which face, its photo, and the box to crop to. */
data class ExcludedFaceUi(
    val faceId: String,
    val photoKey: String,
    val box: FaceBox,
)

/** The "not this person" marks recorded against one person name, so the screen can label the group. */
data class NotThisPersonGroup(
    val name: String,
    val faces: List<ExcludedFaceUi>,
)

data class FaceExclusionsUiState(
    val isLoading: Boolean = true,
    /** Faces removed from a named person ("Not this person"), grouped by that name. */
    val notThisPerson: List<NotThisPersonGroup> = emptyList(),
    /** Faces dismissed as "not a person" (rejected clusters). */
    val ignored: List<ExcludedFaceUi> = emptyList(),
) {
    val isEmpty: Boolean get() = notThisPerson.isEmpty() && ignored.isEmpty()
}

/**
 * Backs [eu.akoos.photos.presentation.people.FaceExclusionsScreen]: the two kinds of face-recognition
 * exclusion the user can undo. A "Not this person" removal recorded a `not_person` row against the
 * person's name; a "Not a person" dismissal set `rejected` on a cluster's faces. Both are listed here
 * with a face crop, and each undo reverses the underlying DB state so the face is re-evaluated on the
 * next clustering pass. Read once and re-read after an undo; gated like the People surfaces, so with the
 * AI or face switch off the screen reads no face data and shows nothing.
 */
@HiltViewModel
class FaceExclusionsViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val peopleCatalog: PeopleCatalog,
    private val faceIndexing: FaceIndexing,
    private val faceEdits: FaceEdits,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FaceExclusionsUiState())
    val uiState: StateFlow<FaceExclusionsUiState> = _uiState.asStateFlow()

    /** Read both kinds of exclusion. A no-op (empty, not loading) with face recognition off, matching
     *  how the People screen gates on the same two switches. */
    fun load() {
        viewModelScope.launch {
            val account = accountManager.getPrimaryUserId().first()?.id ?: LOCAL_PARTITION
            val faceOn = settingsStore.get(Settings.AI_FEATURES_ENABLED) &&
                settingsStore.get(Settings.FACE_ENABLED)
            if (!faceOn) {
                _uiState.value = FaceExclusionsUiState(isLoading = false)
                return@launch
            }
            val groups = peopleCatalog.notPersonFacesForUser(account)
                .groupBy { it.personName }
                .map { (name, rows) ->
                    NotThisPersonGroup(
                        name = name,
                        faces = rows.map {
                            ExcludedFaceUi(it.faceId, it.photoKey, FaceBox(it.left, it.top, it.right, it.bottom))
                        },
                    )
                }
                .sortedBy { it.name.lowercase() }
            val ignored = peopleCatalog.rejectedFacesForUser(account)
                .map { ExcludedFaceUi(it.id, it.photoKey, FaceBox(it.left, it.top, it.right, it.bottom)) }
            _uiState.value = FaceExclusionsUiState(isLoading = false, notThisPerson = groups, ignored = ignored)
        }
    }

    /** Undo a "Not this person" removal: drop the mark so the face can rejoin that name, then nudge a
     *  reindex so the change is re-evaluated without waiting for the next automatic pass. */
    fun undoNotThisPerson(name: String, faceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = accountManager.getPrimaryUserId().first()
            val account = userId?.id ?: LOCAL_PARTITION
            faceEdits.edit {
                peopleCatalog.deleteNotPersonMark(account, name, faceId)
                peopleCatalog.noteNotPerson(account, name, listOf(faceId), removed = true, System.currentTimeMillis())
            }
            faceIndexing.requestIndex(userId)
            load()
        }
    }

    /** Undo a "Not a person" dismissal: un-reject the face (and clear its stale person link) so it
     *  re-clusters, then nudge a reindex. */
    fun undoIgnored(faceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = accountManager.getPrimaryUserId().first()
            faceEdits.edit { peopleCatalog.unrejectFace(faceId) }
            faceIndexing.requestIndex(userId)
            load()
        }
    }
}
