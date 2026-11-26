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

package eu.akoos.photos.data.duplicates

import eu.akoos.photos.data.repository.LocalContentHashFiller
import eu.akoos.photos.data.repository.drive.PerceptualHashScheduler
import eu.akoos.photos.domain.duplicates.LocalContentHashes
import eu.akoos.photos.domain.duplicates.PerceptualHashing
import eu.akoos.photos.domain.entity.GalleryItem
import kotlinx.coroutines.flow.StateFlow
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerceptualHashingImpl @Inject constructor(
    private val scheduler: PerceptualHashScheduler,
) : PerceptualHashing {
    override fun request(items: List<GalleryItem>, userId: UserId?) = scheduler.request(items, userId)
}

@Singleton
class LocalContentHashesImpl @Inject constructor(
    private val filler: LocalContentHashFiller,
) : LocalContentHashes {
    override val hashes: StateFlow<Map<String, String>> get() = filler.hashes
    override fun request(items: List<GalleryItem>) = filler.request(items)
}
