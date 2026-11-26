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

@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package eu.akoos.photos.domain.usecase

import eu.akoos.photos.domain.entity.GalleryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.ConcurrentHashMap

/**
 * Streams the account's photos grouped into countries and cities for the Places surfaces, reusing the
 * same location rows and merged library the map plots from. Every located photo resolves to its
 * [GalleryItem] (so a place card has a cover and a "newest" order) and is reverse-geocoded off the main
 * thread to a city + country; the grouping runs in [groupPlaces].
 *
 * A per-location label cache makes a re-emit cheap: the coordinate is stable for a `photo_location` id,
 * so once a row is geocoded a later emit (a library or thumbnail change) reuses the label instead of
 * rescanning the dataset. A signed-out session has no location rows, so Places is simply empty.
 */
interface ObservePlacesUseCase {
    operator fun invoke(): Flow<PlacesData>
}
