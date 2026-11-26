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

package eu.akoos.photos.data.updater

import eu.akoos.photos.domain.updater.DownloadProgress as DomainDownloadProgress
import eu.akoos.photos.domain.updater.InstallOutcome as DomainInstallOutcome
import eu.akoos.photos.domain.updater.StagedUpdates
import eu.akoos.photos.domain.updater.UpdateDownload
import eu.akoos.photos.domain.updater.UpdateInstall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import android.content.Intent
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StagedUpdatesImpl @Inject constructor(
    private val store: StagedUpdateStore,
) : StagedUpdates {
    override suspend fun record(versionName: String, file: File) = store.record(versionName, file)
    override suspend fun claimFor(versionName: String?): File? = store.claimFor(versionName)
    override suspend fun discard() = store.discard()
}

@Singleton
class UpdateDownloadImpl @Inject constructor(
    private val downloader: UpdateDownloader,
) : UpdateDownload {
    override fun download(apkUrl: String, assetName: String): Flow<DomainDownloadProgress> =
        downloader.download(apkUrl, assetName).map { progress ->
            when (progress) {
                is DownloadProgress.Downloading -> DomainDownloadProgress.Downloading(progress.percent)
                is DownloadProgress.Complete -> DomainDownloadProgress.Complete(progress.file)
                is DownloadProgress.Failed -> DomainDownloadProgress.Failed(progress.cause)
            }
        }
}

@Singleton
class UpdateInstallImpl @Inject constructor(
    private val installer: UpdateInstaller,
) : UpdateInstall {
    private val confirmationIntent = AtomicReference<Intent?>(null)

    /** Android host reads this after [installViaSession] returns PendingUserAction. */
    fun consumeConfirmationIntent(): Intent? = confirmationIntent.getAndSet(null)

    override fun canInstall(): Boolean = installer.canInstall()
    override fun installViaSession(apkFile: File): DomainInstallOutcome {
        confirmationIntent.set(null)
        return when (val outcome = installer.installViaSession(apkFile)) {
            is InstallOutcome.Committed -> DomainInstallOutcome.Committed
            is InstallOutcome.Success -> DomainInstallOutcome.Success
            is InstallOutcome.PendingUserAction -> {
                confirmationIntent.set(outcome.intent)
                DomainInstallOutcome.PendingUserAction
            }
            is InstallOutcome.SignatureMismatch -> DomainInstallOutcome.SignatureMismatch
            is InstallOutcome.NotNewer -> DomainInstallOutcome.NotNewer
            is InstallOutcome.Failed -> DomainInstallOutcome.Failed(outcome.message)
        }
    }
    override fun verifyApkSignature(apkFile: File): Boolean = installer.verifyApkSignature(apkFile)
}
