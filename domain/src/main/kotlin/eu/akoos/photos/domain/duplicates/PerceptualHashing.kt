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

package eu.akoos.photos.domain.duplicates

import eu.akoos.photos.domain.entity.GalleryItem
import kotlinx.coroutines.flow.StateFlow
import me.proton.core.domain.entity.UserId

/** Schedules perceptual (PDQ) fingerprint work for the duplicate finder. */
interface PerceptualHashing {
    fun request(items: List<GalleryItem>, userId: UserId?)
}

/** On-device SHA-1 content hashes for local duplicate candidates. */
interface LocalContentHashes {
    val hashes: StateFlow<Map<String, String>>
    fun request(items: List<GalleryItem>)
}
