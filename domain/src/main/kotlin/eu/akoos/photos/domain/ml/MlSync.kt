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

package eu.akoos.photos.domain.ml

import kotlinx.coroutines.flow.StateFlow
import me.proton.core.domain.entity.UserId

enum class MlSyncStart {
    STARTED,
    ALREADY_RUNNING,
    NOT_AVAILABLE,
    NO_FILES,
    FOLDER_MISSING,
}

enum class FaceSnapshotMode { Merge, Replace }

data class MlRemoteSnapshot(
    val kind: String,
    val deviceId: String,
    val isThisDevice: Boolean,
    val deviceLabel: String?,
    val serverModifiedAtMs: Long,
    val sizeBytes: Long,
    val revisionUid: String,
    val savedAtMs: Long = serverModifiedAtMs,
)

sealed interface MlSyncUiState {
    data object Idle : MlSyncUiState
    data class Running(val operation: String?, val done: Int, val total: Int) : MlSyncUiState
    data class Finished(val ok: Boolean, val message: String?) : MlSyncUiState
}

/** Face/search data Drive sync controller. */
interface MlSync {
    val state: StateFlow<MlSyncUiState>
    fun isAvailable(userId: UserId?): Boolean
    suspend fun listRemote(userId: UserId): List<MlRemoteSnapshot>
    suspend fun restoreNeedsModeChoice(userId: UserId): Boolean
    suspend fun save(userId: UserId, createFolderConfirmed: Boolean): MlSyncStart
    suspend fun restore(userId: UserId, files: List<MlRemoteSnapshot>, mode: FaceSnapshotMode): MlSyncStart
    fun cancel()
    fun dismissResult()
}
