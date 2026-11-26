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

package eu.akoos.photos.domain.updater

import kotlinx.coroutines.flow.Flow
import java.io.File

sealed class DownloadProgress {
    data class Downloading(val percent: Int) : DownloadProgress()
    data class Complete(val file: File) : DownloadProgress()
    data class Failed(val cause: Throwable) : DownloadProgress()
}

/**
 * Outcome of an update install without Android Intent payloads.
 * [PendingUserAction] is a marker; the host obtains the confirmation Intent from the
 * Android-bound installer implementation when needed.
 */
sealed class InstallOutcome {
    data object Committed : InstallOutcome()
    data object Success : InstallOutcome()
    data object PendingUserAction : InstallOutcome()
    data object SignatureMismatch : InstallOutcome()
    data object NotNewer : InstallOutcome()
    data class Failed(val message: String?) : InstallOutcome()
}

interface StagedUpdates {
    suspend fun record(versionName: String, file: File)
    suspend fun claimFor(versionName: String?): File?
    suspend fun discard()
}

interface UpdateDownload {
    fun download(apkUrl: String, assetName: String): Flow<DownloadProgress>
}

interface UpdateInstall {
    fun canInstall(): Boolean
    fun installViaSession(apkFile: File): InstallOutcome
    fun verifyApkSignature(apkFile: File): Boolean
}
