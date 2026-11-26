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

/**
 * Pure helpers for the cloud linkIds every listing (gallery, widget, ML snapshot) leaves out:
 * photos hidden one by one, members of hidden albums, and Drive copies of vaulted photos.
 */
object HiddenCloudIds {

    fun fromParts(
        individuallyHidden: Set<String>,
        hiddenAlbumMembers: Set<String>,
        hiddenUriCloudIdEntries: Set<String>,
        vaultedRowCloudIds: Collection<String>,
    ): Set<String> =
        individuallyHidden + hiddenAlbumMembers + vaultedCloudIds(hiddenUriCloudIdEntries, vaultedRowCloudIds)

    fun vaultedCloudIds(
        hiddenUriCloudIdEntries: Set<String>,
        vaultedRowCloudIds: Collection<String>,
    ): Set<String> {
        val ids = HashSet<String>(hiddenUriCloudIdEntries.size + vaultedRowCloudIds.size)
        for (entry in hiddenUriCloudIdEntries) {
            val linkId = entry.substringAfter('|', "")
            if (linkId.isNotBlank()) ids += linkId
        }
        vaultedRowCloudIds.filterTo(ids) { it.isNotBlank() }
        return ids
    }
}
