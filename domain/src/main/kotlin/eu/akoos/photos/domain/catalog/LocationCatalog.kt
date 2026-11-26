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

package eu.akoos.photos.domain.catalog

import kotlinx.coroutines.flow.Flow

/** Photo GPS and day-meta calendar reads/writes. */
interface LocationCatalog {
    fun observeLocationsForUser(userId: String): Flow<List<PhotoLocationRecord>>
    suspend fun deleteLocationsByIds(userId: String, photoIds: List<String>)
    suspend fun locationForPhoto(userId: String, photoId: String): PhotoLocationRecord?

    fun observeAllDayMeta(userId: String): Flow<List<DayMetaRecord>>
    fun observeDayMetaByDate(userId: String, date: String): Flow<DayMetaRecord?>
    suspend fun getAllDayMeta(userId: String): List<DayMetaRecord>
    suspend fun getDayMetaByDate(userId: String, date: String): DayMetaRecord?
    suspend fun upsertDayMeta(record: DayMetaRecord)
}
