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

/** Pure vault hide decisions presentation needs without importing data.hidden. */
object VaultDecisions {
    /** Originals whose vault copies landed, so the hide may delete them. */
    fun deletableOriginals(
        vaultable: List<VaultHideTarget>,
        stored: List<VaultJournalEntry>,
    ): List<GalleryItem> {
        if (stored.isEmpty() || vaultable.isEmpty()) return emptyList()
        val copied = stored.mapTo(HashSet(stored.size)) { it.sourceUri }
        return vaultable.filter { it.local.uri in copied }.map { it.item }
    }
}
