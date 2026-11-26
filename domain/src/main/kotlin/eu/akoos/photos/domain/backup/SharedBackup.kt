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


package eu.akoos.photos.domain.backup

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId

data class SharedBackupStatus(val enabled: Boolean, val lastAtMs: Long, val lastFailed: Boolean)

data class DriveDeviceCopies(
    val deviceId: String,
    val name: String?,
    val lastSavedMs: Long,
    val nodeUids: List<String>,
)

/** Cross-device shared backup of faces/search/labels/hidden. */
interface SharedBackup {
    val status: Flow<SharedBackupStatus>
    /** True while a shared-backup ML sync ROUND is running. */
    fun isRunning(mlSyncRunning: Boolean): Boolean = mlSyncRunning

    suspend fun setEnabled(enabled: Boolean)
    suspend fun saveNow()
    suspend fun loadNow()
    suspend fun devices(userId: UserId): List<DriveDeviceCopies>?
    suspend fun trash(userId: UserId, device: DriveDeviceCopies): Boolean
    suspend fun deviceId(): String?
    fun deleteFacesOnlyHere(userId: UserId)
    fun deleteFacesEverywhere(userId: UserId)
}
