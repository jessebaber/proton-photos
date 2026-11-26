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

/**
 * The stored-fix ids a landed strip with [stripGps] over [strippedUris] makes stale.
 *
 * A strip that leaves GPS alone never touches the coordinates in the file, so the stored fix still
 * describes the bytes and has to stand: a timestamp-only strip invalidates nothing. And every id is a
 * device content URI, the key `photo_location` records a local fix under. Pure, so it is verifiable
 * without a ViewModel or a database.
 */
fun strippedLocationIds(stripGps: Boolean, strippedUris: Collection<String>): List<String> =
    if (!stripGps) emptyList() else strippedUris.distinct()

/**
 * Drops the stored GPS fix of the device files a strip just removed location from.
 *
 * [stripGps] is the GPS flag of the strip config that landed; presentation passes
 * `config.stripGps` so this API stays free of app util types.
 */
interface InvalidateStrippedLocationsUseCase {
    suspend operator fun invoke(stripGps: Boolean, strippedUris: Collection<String>)
}
