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

import eu.akoos.photos.domain.entity.GalleryItem
import eu.akoos.photos.domain.entity.LocalMediaItem

/** One photo on its way into the vault. */
data class VaultHideTarget(
    val item: GalleryItem,
    val local: LocalMediaItem,
    val cloudLinkId: String?,
    val sizeBytes: Long,
) {
    val captureTimeMs: Long get() = item.captureTimeMs
}

/** Device files to vault plus cloud link ids to filter-hide. */
data class VaultHideSplit(
    val vaultable: List<VaultHideTarget>,
    val cloudLinkIds: List<String>,
) : HideConfirmSource {
    override val isEmpty: Boolean get() = vaultable.isEmpty() && cloudLinkIds.isEmpty()
    override val vaultCount: Int get() = vaultable.size
    override val cloudCopyCount: Int get() = vaultable.count { it.cloudLinkId != null }
    override val cloudOnlyCount: Int get() = cloudLinkIds.size

    companion object {
        val EMPTY = VaultHideSplit(emptyList(), emptyList())
    }
}

/** Pure folder/vault partitioning helpers presentation uses when hiding a device folder. */
interface HiddenFolders {
    fun hideSplit(
        items: List<GalleryItem>,
        cloudLinkIdByUri: Map<String, String> = emptyMap(),
    ): VaultHideSplit

    fun folderHideSplit(
        items: List<GalleryItem>,
        bucketName: String,
        cloudLinkIdByUri: Map<String, String> = emptyMap(),
    ): VaultHideSplit

    fun folderPhotos(
        live: List<GalleryItem>,
        vaulted: List<GalleryItem.LocalOnly>,
        showsVault: Boolean,
    ): List<GalleryItem>

    fun vaultedByBucket(
        sourceFolderEntries: Set<String>,
        vaultedUris: Set<String>,
    ): Map<String, List<String>>

    fun looseVaultedUris(
        vaultedUris: Set<String>,
        sourceFolderEntries: Set<String>,
        hiddenFolderNames: Set<String>,
    ): Set<String>
}
