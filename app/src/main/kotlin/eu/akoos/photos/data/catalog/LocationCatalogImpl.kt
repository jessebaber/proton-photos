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

package eu.akoos.photos.data.catalog

import eu.akoos.photos.data.db.dao.DayMetaDao
import eu.akoos.photos.data.db.dao.PhotoLocationDao
import eu.akoos.photos.data.db.entity.DayMetaEntity
import eu.akoos.photos.domain.catalog.CatalogMappers
import eu.akoos.photos.domain.catalog.DayMetaRecord
import eu.akoos.photos.domain.catalog.LocationCatalog
import eu.akoos.photos.domain.catalog.PhotoLocationRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationCatalogImpl @Inject constructor(
    private val photoLocationDao: PhotoLocationDao,
    private val dayMetaDao: DayMetaDao,
) : LocationCatalog {
    override fun observeLocationsForUser(userId: String): Flow<List<PhotoLocationRecord>> =
        photoLocationDao.observeForUser(userId).map { list ->
            list.map { PhotoLocationRecord(it.id, it.userId, it.latitude, it.longitude) }
        }

    override suspend fun deleteLocationsByIds(userId: String, photoIds: List<String>) {
        photoLocationDao.deleteByIds(userId, photoIds)
    }

    override suspend fun locationForPhoto(userId: String, photoId: String): PhotoLocationRecord? =
        photoLocationDao.getById(userId, photoId)?.let {
            PhotoLocationRecord(it.id, it.userId, it.latitude, it.longitude)
        }

    override fun observeAllDayMeta(userId: String): Flow<List<DayMetaRecord>> =
        dayMetaDao.observeAll(userId).map { list -> list.map { it.toRecord() } }

    override fun observeDayMetaByDate(userId: String, date: String): Flow<DayMetaRecord?> =
        dayMetaDao.observeByDate(userId, date).map { it?.toRecord() }

    override suspend fun getAllDayMeta(userId: String): List<DayMetaRecord> =
        dayMetaDao.getAll(userId).map { it.toRecord() }

    override suspend fun getDayMetaByDate(userId: String, date: String): DayMetaRecord? =
        dayMetaDao.getByDate(userId, date)?.toRecord()

    override suspend fun upsertDayMeta(record: DayMetaRecord) {
        dayMetaDao.upsert(
            DayMetaEntity(
                date = record.date,
                userId = record.userId,
                coverPhotoUri = record.coverPhotoUri,
                locationText = record.locationText,
                description = record.description,
                updatedAt = record.updatedAt,
            ),
        )
    }

    private fun DayMetaEntity.toRecord() = CatalogMappers.dayMetaRecord(
        date, userId, coverPhotoUri, locationText, description, updatedAt,
    )
}
