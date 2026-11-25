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

package eu.akoos.photos.domain.vault

import java.io.OutputStream

/**
 * Private vault blob store for hidden photos.
 * Implementations hold Context; callers pass string URIs only.
 */
interface HiddenStorage {
    fun clearVault(keep: Set<String> = emptySet())
    fun vaultBlobUris(): Set<String>
    fun vaultSizeBytes(): Long
    fun sourceFolderFor(srcUri: String, fallbackBucketName: String?): String?
    fun hasBlob(hiddenUri: String): Boolean
    fun isHiddenUri(uri: String): Boolean

    suspend fun store(
        srcUri: String,
        originalDisplayName: String? = null,
        mimeType: String? = null,
        captureTimeMs: Long? = null,
    ): String?

    suspend fun create(
        extension: String,
        captureTimeMs: Long?,
        write: (OutputStream) -> Unit,
    ): String?

    suspend fun restore(
        hiddenUri: String,
        originalDisplayName: String? = null,
        albumFolderName: String? = null,
        onRestoredUri: suspend (String) -> Boolean = { true },
    ): RestoredFile?

    fun delete(hiddenUri: String): Boolean
    suspend fun rename(hiddenUri: String, newName: String): String?
    suspend fun restampCaptureTime(hiddenUri: String, captureTimeMs: Long): String?
    suspend fun duplicate(hiddenUri: String): String?

    data class RestoredFile(
        val uri: String,
        val viaOriginalPath: Boolean,
        val recordsSettled: Boolean,
    )
}
