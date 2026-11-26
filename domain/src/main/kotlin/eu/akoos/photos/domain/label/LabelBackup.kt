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

enum class LabelBackupOperation { SAVE, RESTORE }

enum class LabelBackupStep { PREPARING, ENCRYPTING, UPLOADING, DOWNLOADING, MERGING }

data class LabelBackupProgress(val step: LabelBackupStep) {
    companion object {
        val QUEUED = LabelBackupProgress(LabelBackupStep.PREPARING)
    }
}

enum class LabelBackupResult {
    SAVED, NOTHING_TO_SAVE, RESTORED, NOT_SIGNED_IN, NOT_CAPABLE,
    WRONG_ACCOUNT, TOO_NEW, DAMAGED, CONFLICT, FOLDER_MISSING, FAILED,
}

data class LabelBackupOutcome(
    val operation: LabelBackupOperation,
    val result: LabelBackupResult,
    val photos: Int = 0,
    val newLabels: Int = 0,
)

data class LabelCount(val label: String, val photos: Int)

data class LabelSavedCopy(
    val deviceId: String,
    val deviceLabel: String,
    val isThisDevice: Boolean,
    val savedAtMillis: Long,
    val revisionUid: String,
    val labels: List<LabelCount>,
) {
    val labelCount: Int get() = labels.size
}

sealed interface LabelBackupState {
    data object Idle : LabelBackupState
    data class Running(
        val operation: LabelBackupOperation,
        val progress: LabelBackupProgress = LabelBackupProgress.QUEUED,
    ) : LabelBackupState
    data class Finished(val runId: String, val outcome: LabelBackupOutcome) : LabelBackupState
}

sealed interface LabelBackupCopies {
    data class Found(val copies: List<LabelSavedCopy>) : LabelBackupCopies
    data object Empty : LabelBackupCopies
    data object NotCapable : LabelBackupCopies
    data object Failed : LabelBackupCopies
}

interface LabelBackup {
    val state: StateFlow<LabelBackupState>
    fun isAvailable(userId: UserId?): Boolean
    suspend fun save(userId: UserId, createFolderConfirmed: Boolean): LabelBackupStart
    suspend fun listCopies(userId: UserId): LabelBackupCopies
    suspend fun restore(userId: UserId, copy: LabelSavedCopy): LabelBackupStart
    fun cancel()
    fun dismissResult()
}
