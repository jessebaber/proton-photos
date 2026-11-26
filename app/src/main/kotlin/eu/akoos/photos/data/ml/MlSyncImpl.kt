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

package eu.akoos.photos.data.ml

import eu.akoos.photos.data.mlsync.FaceSnapshotMode as DataFaceMode
import eu.akoos.photos.data.mlsync.MlDriveResult
import eu.akoos.photos.data.mlsync.MlRemoteSnapshot as DataSnapshot
import eu.akoos.photos.data.mlsync.MlSyncController
import eu.akoos.photos.data.mlsync.MlSyncOutcome
import eu.akoos.photos.data.mlsync.MlSyncResult
import eu.akoos.photos.data.mlsync.MlSyncStart as DataStart
import eu.akoos.photos.data.mlsync.MlSyncState
import eu.akoos.photos.di.AppScope
import eu.akoos.photos.domain.ml.FaceSnapshotMode
import eu.akoos.photos.domain.ml.MlRemoteSnapshot
import eu.akoos.photos.domain.ml.MlSync
import eu.akoos.photos.domain.ml.MlSyncStart
import eu.akoos.photos.domain.ml.MlSyncUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MlSyncImpl @Inject constructor(
    private val controller: MlSyncController,
    @AppScope private val appScope: CoroutineScope,
) : MlSync {
    override val state: StateFlow<MlSyncUiState> = controller.state
        .map { it.toDomain() }
        .stateIn(appScope, SharingStarted.Eagerly, MlSyncUiState.Idle)

    override fun isAvailable(userId: UserId?): Boolean = controller.isAvailable(userId)

    override suspend fun listRemote(userId: UserId): List<MlRemoteSnapshot> =
        when (val result = controller.listRemote(userId)) {
            is MlDriveResult.Ok -> result.value.map { it.toDomain() }
            else -> emptyList()
        }

    override suspend fun restoreNeedsModeChoice(userId: UserId): Boolean =
        controller.restoreNeedsModeChoice(userId)

    override suspend fun save(userId: UserId, createFolderConfirmed: Boolean): MlSyncStart =
        controller.save(userId, createFolderConfirmed).toDomain()

    override suspend fun restore(
        userId: UserId,
        files: List<MlRemoteSnapshot>,
        mode: FaceSnapshotMode,
    ): MlSyncStart {
        val dataMode = when (mode) {
            FaceSnapshotMode.Merge -> DataFaceMode.Merge
            FaceSnapshotMode.Replace -> DataFaceMode.Replace
        }
        val remote = when (val result = controller.listRemote(userId)) {
            is MlDriveResult.Ok -> result.value
            else -> emptyList()
        }
        val keys = files.map { Triple(it.kind, it.deviceId, it.revisionUid) }.toSet()
        val selected = remote.filter { Triple(it.kind.name, it.deviceId, it.revisionUid) in keys }
        return controller.restore(userId, selected, dataMode).toDomain()
    }

    override fun cancel() = controller.cancel()
    override fun dismissResult() = controller.dismissResult()

    private fun MlSyncState.toDomain(): MlSyncUiState = when (this) {
        MlSyncState.Idle -> MlSyncUiState.Idle
        is MlSyncState.Running -> MlSyncUiState.Running(
            operation = operation?.name,
            done = progress.done,
            total = progress.total,
        )
        is MlSyncState.Finished -> MlSyncUiState.Finished(
            ok = outcome.result == MlSyncResult.SUCCESS,
            message = outcome.failure,
        )
    }

    private fun DataStart.toDomain() = when (this) {
        DataStart.STARTED -> MlSyncStart.STARTED
        DataStart.ALREADY_RUNNING -> MlSyncStart.ALREADY_RUNNING
        DataStart.NOT_AVAILABLE -> MlSyncStart.NOT_AVAILABLE
        DataStart.NO_FILES -> MlSyncStart.NO_FILES
        DataStart.FOLDER_MISSING -> MlSyncStart.FOLDER_MISSING
    }

    private fun DataSnapshot.toDomain() = MlRemoteSnapshot(
        kind = kind.name,
        deviceId = deviceId,
        isThisDevice = isThisDevice,
        deviceLabel = deviceLabel,
        serverModifiedAtMs = serverModifiedAtMs,
        sizeBytes = sizeBytes,
        revisionUid = revisionUid,
        savedAtMs = savedAtMs,
    )
}
