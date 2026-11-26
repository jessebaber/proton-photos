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

package eu.akoos.photos.domain.label

import kotlinx.coroutines.flow.StateFlow
import me.proton.core.domain.entity.UserId

enum class LabelBackupStart { STARTED, ALREADY_RUNNING, NOT_AVAILABLE, FOLDER_MISSING }

data class LabelSavedCopy(
    val deviceId: String,
    val deviceLabel: String,
    val isThisDevice: Boolean,
    val savedAtMillis: Long,
    val revisionUid: String,
    val labelCount: Int,
)

sealed interface LabelBackupUiState {
    data object Idle : LabelBackupUiState
    data class Running(val operation: String, val step: String) : LabelBackupUiState
    data class Finished(val ok: Boolean, val result: String) : LabelBackupUiState
}

sealed interface LabelBackupCopies {
    data class Found(val copies: List<LabelSavedCopy>) : LabelBackupCopies
    data object Empty : LabelBackupCopies
    data object NotCapable : LabelBackupCopies
    data object Failed : LabelBackupCopies
}

interface LabelBackup {
    val state: StateFlow<LabelBackupUiState>
    fun isAvailable(userId: UserId?): Boolean
    suspend fun save(userId: UserId, createFolderConfirmed: Boolean): LabelBackupStart
    suspend fun listCopies(userId: UserId): LabelBackupCopies
    suspend fun restore(userId: UserId, copy: LabelSavedCopy): LabelBackupStart
    fun cancel()
    fun dismissResult()
}
