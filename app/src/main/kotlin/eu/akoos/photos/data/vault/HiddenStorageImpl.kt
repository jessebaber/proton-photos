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

package eu.akoos.photos.data.vault

import eu.akoos.photos.data.hidden.HiddenStorageManager
import eu.akoos.photos.domain.vault.HiddenStorage
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HiddenStorageImpl @Inject constructor(
    private val manager: HiddenStorageManager,
) : HiddenStorage {
    override fun clearVault(keep: Set<String>) = manager.clearVault(keep)
    override fun vaultBlobUris(): Set<String> = manager.vaultBlobUris()
    override fun vaultSizeBytes(): Long = manager.vaultSizeBytes()
    override fun sourceFolderFor(srcUri: String, fallbackBucketName: String?): String? =
        manager.sourceFolderFor(srcUri, fallbackBucketName)
    override fun hasBlob(hiddenUri: String): Boolean = manager.hasBlob(hiddenUri)
    override fun isHiddenUri(uri: String): Boolean = manager.isHiddenUri(uri)

    override suspend fun store(
        srcUri: String,
        originalDisplayName: String?,
        mimeType: String?,
        captureTimeMs: Long?,
    ): String? = manager.store(srcUri, originalDisplayName, mimeType, captureTimeMs)

    override suspend fun create(
        extension: String,
        captureTimeMs: Long?,
        write: (OutputStream) -> Unit,
    ): String? = manager.create(extension, captureTimeMs, write)

    override suspend fun restore(
        hiddenUri: String,
        originalDisplayName: String?,
        albumFolderName: String?,
        onRestoredUri: suspend (String) -> Boolean,
    ): HiddenStorage.RestoredFile? {
        val restored = manager.restore(hiddenUri, originalDisplayName, albumFolderName, onRestoredUri)
            ?: return null
        return HiddenStorage.RestoredFile(
            uri = restored.uri,
            viaOriginalPath = restored.viaOriginalPath,
            recordsSettled = restored.recordsSettled,
        )
    }

    override fun delete(hiddenUri: String): Boolean = manager.delete(hiddenUri)
    override suspend fun rename(hiddenUri: String, newName: String): String? = manager.rename(hiddenUri, newName)
    override suspend fun restampCaptureTime(hiddenUri: String, captureTimeMs: Long): String? =
        manager.restampCaptureTime(hiddenUri, captureTimeMs)
    override suspend fun duplicate(hiddenUri: String): String? = manager.duplicate(hiddenUri)
}
