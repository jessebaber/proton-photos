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
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId

/**
 * Merges device and Drive photos into the gallery timeline feed every screen reads.
 * Android/DataStore/Room details stay in the data-layer implementation.
 */
interface GetGalleryItemsUseCase {
    fun invoke(userId: UserId): Flow<List<GalleryItem>>

    /**
     * [full] with a first list put ahead of it for a screen that fills in as soon as it can.
     * For a screen that shows the library, never one that acts on what is absent from it.
     */
    fun withQuickStart(userId: UserId?, full: Flow<List<GalleryItem>>): Flow<List<GalleryItem>>

    /** Session-free gallery feed for the no-account (local-only) mode. */
    fun invokeLocalOnly(): Flow<List<GalleryItem>>
}
