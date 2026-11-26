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

import eu.akoos.photos.data.hidden.HiddenFolderRecords
import eu.akoos.photos.domain.entity.GalleryItem
import eu.akoos.photos.domain.vault.HiddenFolders
import eu.akoos.photos.domain.vault.VaultHideSplit
import eu.akoos.photos.domain.vault.VaultHideTarget
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HiddenFoldersImpl @Inject constructor() : HiddenFolders {
    override fun hideSplit(
        items: List<GalleryItem>,
        cloudLinkIdByUri: Map<String, String>,
    ): VaultHideSplit = HiddenFolderRecords.hideSplit(items, cloudLinkIdByUri).toDomain()

    override fun folderHideSplit(
        items: List<GalleryItem>,
        bucketName: String,
        cloudLinkIdByUri: Map<String, String>,
    ): VaultHideSplit = HiddenFolderRecords.folderHideSplit(items, bucketName, cloudLinkIdByUri).toDomain()

    override fun folderPhotos(
        live: List<GalleryItem>,
        vaulted: List<GalleryItem.LocalOnly>,
        showsVault: Boolean,
    ): List<GalleryItem> = HiddenFolderRecords.folderPhotos(live, vaulted, showsVault)

    override fun vaultedByBucket(
        sourceFolderEntries: Set<String>,
        vaultedUris: Set<String>,
    ): Map<String, List<String>> = HiddenFolderRecords.vaultedByBucket(sourceFolderEntries, vaultedUris)

    override fun looseVaultedUris(
        vaultedUris: Set<String>,
        sourceFolderEntries: Set<String>,
        hiddenFolderNames: Set<String>,
    ): Set<String> = HiddenFolderRecords.looseVaultedUris(vaultedUris, sourceFolderEntries, hiddenFolderNames)

    private fun HiddenFolderRecords.HideSplit.toDomain() = VaultHideSplit(
        vaultable = vaultable.map {
            VaultHideTarget(it.item, it.local, it.cloudLinkId, it.sizeBytes)
        },
        cloudLinkIds = cloudLinkIds,
    )
}
