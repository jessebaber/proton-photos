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

import kotlinx.coroutines.flow.StateFlow

/** Read/write of the persisted vault index and per-uri maps. */
interface HiddenVaultIndex {
    fun pairedUris(rawPairs: Set<String>): Set<String>
    fun recordedName(displayName: String, ext: String): String
}

/** Editor that mutates vault blobs after metadata / edit flows. */
interface HiddenVaultEdit {
    val moves: StateFlow<Map<String, VaultMove>>
    suspend fun rename(uri: String, newName: String): String?
    suspend fun copy(uri: String, newName: String): String?
    suspend fun adoptCopy(sourceUri: String, copyUri: String, displayName: String)
    suspend fun restampCaptureTime(uri: String, captureTimeMs: Long): String?
}

/** Restores or deletes a vaulted photo through the shared restorer path. */
interface HiddenVaultRestore {
    suspend fun deletePhoto(hiddenUri: String): Boolean
    suspend fun restorePhoto(hiddenUri: String, fallbackDisplayName: String? = null): Boolean
    suspend fun restoreAll(hiddenUris: List<String>): Int
    suspend fun restoreFolder(
        bucketName: String,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
        shouldStop: () -> Boolean = { false },
    ): Int
}


/** Leftover vault file discovery after sign-out / wipe. */
interface HiddenVaultLeftover {
    fun vaultedUris(blobUris: Set<String>, recordedVaultUris: Set<String>): Set<String>
}
