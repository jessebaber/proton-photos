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

enum class MlDriveFileKind(val prefix: String) { FACES("faces"), SEARCH("search") }

enum class MlSyncOperation { SAVE, RESTORE, ROUND }

enum class MlSyncPhase {
    PREPARING, EXPORTING, UPLOADING, DOWNLOADING, IMPORTING, FINISHING,
}

enum class MlSyncPartStatus {
    DONE, EMPTY, NAMES_ONLY, MODEL_MISMATCH, BEFORE_WIPE, WRONG_ACCOUNT,
    TOO_NEW, DAMAGED, CONFLICT, NO_SPACE, FAILED,
    ;
    val finished: Boolean
        get() = this == DONE || this == NAMES_ONLY || this == MODEL_MISMATCH || this == BEFORE_WIPE
    val failed: Boolean
        get() = !finished && this != EMPTY
}

enum class MlSyncResult {
    SUCCESS, PARTIAL, NOTHING_TO_SAVE, NOT_SIGNED_IN, NOT_CAPABLE,
    FOLDER_MISSING, INVALID_REQUEST, NO_SPACE, FAILED,
}

data class MlSyncProgress(
    val phase: MlSyncPhase,
    val kind: MlDriveFileKind?,
    val done: Int,
    val total: Int,
) {
    companion object {
        val QUEUED = MlSyncProgress(MlSyncPhase.PREPARING, kind = null, done = 0, total = 0)
    }
}

data class MlSyncCounts(
    val faces: Int = 0,
    val people: Int = 0,
    val scanMarkers: Int = 0,
    val searchEmbeddings: Int = 0,
    val searchKept: Int = 0,
    val filesDone: Int = 0,
    val filesTotal: Int = 0,
)

data class MlSyncOutcome(
    val operation: MlSyncOperation?,
    val result: MlSyncResult,
    val faces: MlSyncPartStatus? = null,
    val search: MlSyncPartStatus? = null,
    val counts: MlSyncCounts = MlSyncCounts(),
    val failure: String? = null,
)

sealed interface MlSyncState {
    data object Idle : MlSyncState
    data class Running(val operation: MlSyncOperation?, val progress: MlSyncProgress) : MlSyncState
    data class Finished(val runId: String, val outcome: MlSyncOutcome) : MlSyncState
}

data class MlRemoteSnapshot(
    val kind: MlDriveFileKind,
    val deviceId: String,
    val isThisDevice: Boolean,
    val deviceLabel: String?,
    val serverModifiedAtMs: Long,
    val sizeBytes: Long,
    val revisionUid: String,
    val savedAtMs: Long = serverModifiedAtMs,
)

const val FACE_WALK_IDLE_TIMEOUT_MS = 2 * 60_000L
const val FACE_INDEX_FOREGROUND_THRESHOLD = 50

/** Whether the face pause is the user's own (switch on, not held by ML sync). */
fun faceIndexingPausedByUser(pauseSwitchOn: Boolean?, heldByMlSync: Boolean?): Boolean =
    pauseSwitchOn == true && heldByMlSync == false

/** Whether an import that stood the walk down should hand it back. */
fun faceWalkResumes(heldByMlSync: Boolean, pausedByUserAtStart: Boolean): Boolean =
    heldByMlSync || !pausedByUserAtStart

/** True when [failure] is an out-of-space failure. */
fun isStorageFull(failure: Throwable): Boolean {
    var t: Throwable? = failure
    while (t != null) {
        val name = t.javaClass.name
        if (name.contains("ENOSPC", ignoreCase = true) ||
            name.contains("NoSpace", ignoreCase = true) ||
            (t.message?.contains("No space", ignoreCase = true) == true)
        ) return true
        t = t.cause
    }
    return false
}

data class FacePauseHold(val switchOn: Boolean, val held: Boolean)

/** Face/search data Drive sync controller. */
interface MlSync {
    val state: StateFlow<MlSyncState>
    fun isAvailable(userId: UserId?): Boolean
    suspend fun listRemote(userId: UserId): List<MlRemoteSnapshot>
    suspend fun restoreNeedsModeChoice(userId: UserId): Boolean
    suspend fun save(userId: UserId, createFolderConfirmed: Boolean): MlSyncStart
    suspend fun restore(userId: UserId, files: List<MlRemoteSnapshot>, mode: FaceSnapshotMode): MlSyncStart
    fun cancel()
    fun dismissResult()

    /** Pause-hold used around face imports / restores. */
    suspend fun readFacePauseHold(): FacePauseHold?
    suspend fun holdFacePause(): Boolean
    suspend fun liftFacePause()
}
