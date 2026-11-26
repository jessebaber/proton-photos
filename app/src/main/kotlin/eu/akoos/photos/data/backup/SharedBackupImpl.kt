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


package eu.akoos.photos.data.backup

import eu.akoos.photos.data.mlsync.MlDriveStore
import eu.akoos.photos.data.sharedbackup.DriveDeviceCopies as DataCopies
import eu.akoos.photos.data.sharedbackup.FaceDataDelete
import eu.akoos.photos.data.sharedbackup.SharedBackupRunner
import eu.akoos.photos.data.sharedbackup.SharedBackupScheduler
import eu.akoos.photos.data.sharedbackup.SharedBackupStore
import eu.akoos.photos.domain.backup.DriveDeviceCopies
import eu.akoos.photos.domain.backup.SharedBackup
import eu.akoos.photos.domain.backup.SharedBackupStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedBackupImpl @Inject constructor(
    private val store: SharedBackupStore,
    private val scheduler: SharedBackupScheduler,
    private val runner: SharedBackupRunner,
    private val driveStore: MlDriveStore,
    private val faceDataDelete: FaceDataDelete,
) : SharedBackup {
    override val status: Flow<SharedBackupStatus> = store.status.map {
        SharedBackupStatus(it.enabled, it.lastAtMs, it.lastFailed)
    }

    override suspend fun setEnabled(enabled: Boolean) = scheduler.setEnabled(enabled)
    override suspend fun saveNow() = scheduler.saveNow()
    override suspend fun loadNow() = scheduler.loadNow()

    override suspend fun devices(userId: UserId): List<DriveDeviceCopies>? =
        runner.devices(userId)?.map { it.toDomain() }

    override suspend fun trash(userId: UserId, device: DriveDeviceCopies): Boolean {
        val data = DataCopies(device.deviceId, device.name, device.lastSavedMs, device.nodeUids)
        return runner.trash(userId, data)
    }

    override suspend fun deviceId(): String? = runCatching { driveStore.deviceId() }.getOrNull()
    override fun deleteFacesOnlyHere(userId: UserId) = faceDataDelete.onlyHere(userId)
    override fun deleteFacesEverywhere(userId: UserId) = faceDataDelete.everywhere(userId)

    private fun DataCopies.toDomain() = DriveDeviceCopies(deviceId, name, lastSavedMs, nodeUids)
}
