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

package eu.akoos.photos.domain.usecase

import eu.akoos.photos.domain.entity.GalleryItem
import eu.akoos.photos.domain.entity.LocalMediaItem

/** [kept] with [live] laid over it: a uri in both is taken from [live], which is read now. */
fun withLiveNewest(kept: List<LocalMediaItem>, live: List<LocalMediaItem>): List<LocalMediaItem> {
    if (live.isEmpty()) return kept
    val liveUris = live.mapTo(HashSet(live.size * 2)) { it.uri }
    return live + kept.filter { it.uri !in liveUris }
}

/** A gallery item belongs to a hidden album when its cloud copy's linkId is a hidden-album
 *  member. A LocalOnly item has no cloud linkId, so an album hide never removes a device-only
 *  photo (those use the separate hidden vault). */
fun isInHiddenAlbum(item: GalleryItem, hiddenLinkIds: Set<String>): Boolean {
    if (hiddenLinkIds.isEmpty()) return false
    val linkId = when (item) {
        is GalleryItem.Synced -> item.cloud.linkId
        is GalleryItem.CloudOnly -> item.cloud.linkId
        is GalleryItem.LocalOnly -> null
    }
    return linkId != null && linkId in hiddenLinkIds
}
