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

import eu.akoos.photos.data.db.dao.FaceCurationTimeDao
import eu.akoos.photos.data.db.dao.FaceDao
import eu.akoos.photos.data.db.dao.NotPersonDao
import eu.akoos.photos.data.db.dao.PersonDao
import eu.akoos.photos.data.db.dao.PersonManualPhotoDao
import eu.akoos.photos.data.db.entity.FaceEntity
import eu.akoos.photos.data.db.entity.PersonEntity
import eu.akoos.photos.domain.catalog.CatalogMappers
import eu.akoos.photos.domain.catalog.FaceBoxRecord
import eu.akoos.photos.domain.catalog.FaceRecord
import eu.akoos.photos.domain.catalog.NotPersonRecord
import eu.akoos.photos.domain.catalog.PeopleCatalog
import eu.akoos.photos.domain.catalog.PersonRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeopleCatalogImpl @Inject constructor(
    private val personDao: PersonDao,
    private val faceDao: FaceDao,
    private val notPersonDao: NotPersonDao,
    private val personManualPhotoDao: PersonManualPhotoDao,
    private val faceCurationTimeDao: FaceCurationTimeDao,
) : PeopleCatalog {
    override suspend fun personById(personId: Long): PersonRecord? =
        personDao.personById(personId)?.toRecord()

    override fun observePersonForUser(userId: String, personId: Long): Flow<PersonRecord?> =
        personDao.observePersonForUser(userId, personId).map { it?.toRecord() }

    override suspend fun namedPeopleForUser(userId: String): List<PersonRecord> =
        personDao.namedPeopleForUser(userId).map { it.toRecord() }

    override suspend fun otherPersonId(userId: String): Long? = personDao.otherPersonId(userId)

    override suspend fun updateCoverAndCount(personId: Long, coverFaceId: String?, faceCount: Int) =
        personDao.updateCoverAndCount(personId, coverFaceId, faceCount)

    override suspend fun keptFacesForPhoto(userId: String, photoKey: String): List<FaceRecord> =
        faceDao.keptFacesForPhoto(userId, photoKey).map { it.toRecord() }

    override suspend fun facesForPerson(userId: String, personId: Long): List<FaceRecord> =
        faceDao.facesForPerson(userId, personId).map { it.toRecord() }

    override suspend fun facesOnPhotos(userId: String, photoKeys: List<String>): List<FaceBoxRecord> =
        faceDao.facesOnPhotos(userId, photoKeys).map {
            FaceBoxRecord(it.id, it.photoKey, it.boxLeft, it.boxTop, it.boxRight, it.boxBottom, it.personId)
        }

    override suspend fun facePhotoKeysIn(userId: String, photoKeys: List<String>): List<String> =
        faceDao.facePhotoKeysIn(userId, photoKeys)

    override fun photoKeysForPerson(userId: String, personId: Long): Flow<List<String>> =
        faceDao.photoKeysForPerson(userId, personId)

    override fun observeFaceBoxesForPerson(userId: String, personId: Long): Flow<List<FaceBoxRecord>> =
        faceDao.observeFaceBoxesForPerson(userId, personId).map { list ->
            list.map {
                FaceBoxRecord(it.id, it.photoKey, it.boxLeft, it.boxTop, it.boxRight, it.boxBottom, it.personId)
            }
        }

    override suspend fun faceIdsForPerson(userId: String, personId: Long): List<String> =
        faceDao.faceIdsForPerson(userId, personId)

    override suspend fun distinctPhotoKeysForPerson(userId: String, personId: Long): List<String> =
        faceDao.distinctPhotoKeysForPerson(userId, personId)

    override suspend fun topFaceForPerson(userId: String, personId: Long): String? =
        faceDao.topFaceForPerson(userId, personId)

    override suspend fun faceCountForPerson(userId: String, personId: Long): Int =
        faceDao.faceCountForPerson(userId, personId)

    override suspend fun upsertFaces(rows: List<FaceRecord>) {
        faceDao.upsert(rows.map { it.toEntity() })
    }

    override suspend fun allNotPersonForUser(userId: String): List<NotPersonRecord> =
        notPersonDao.allForUser(userId).map { NotPersonRecord(it.userId, it.personName, it.faceId) }

    override fun photoKeysForName(userId: String, name: String): Flow<List<String>> =
        personManualPhotoDao.photoKeysForName(userId, name)

    override suspend fun photoKeysForNameList(userId: String, name: String): List<String> =
        personManualPhotoDao.photoKeysForNameList(userId, name)

    override suspend fun clearCurationTimes(userId: String) {
        faceCurationTimeDao.clearForUser(userId)
    }

    private fun PersonEntity.toRecord() = CatalogMappers.personRecord(
        id, userId, displayName, coverFaceId, faceCount, updatedAt, isOther,
    )

    private fun FaceEntity.toRecord() = CatalogMappers.faceRecord(
        id, userId, photoKey, left, top, right, bottom, landmarks, embedding,
        personId, score, blur, rejected, manualName,
    )

    private fun FaceRecord.toEntity() = FaceEntity(
        id, userId, photoKey, left, top, right, bottom, landmarks, embedding,
        personId, score, blur, rejected, manualName,
    )
}
