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

import androidx.room.withTransaction
import eu.akoos.photos.data.db.AppDatabase
import eu.akoos.photos.data.db.dao.ClusterSummaryDao
import eu.akoos.photos.data.db.dao.FaceCurationTimeDao
import eu.akoos.photos.data.db.dao.FaceDao
import eu.akoos.photos.data.db.dao.NotPersonDao
import eu.akoos.photos.data.db.dao.PersonCoverDao
import eu.akoos.photos.data.db.dao.PersonDao
import eu.akoos.photos.data.db.dao.PersonManualPhotoDao
import eu.akoos.photos.data.db.entity.FaceEntity
import eu.akoos.photos.data.db.entity.NotPersonEntity
import eu.akoos.photos.data.db.entity.PersonCoverEntity
import eu.akoos.photos.data.db.entity.PersonEntity
import eu.akoos.photos.data.db.entity.PersonManualPhotoEntity
import eu.akoos.photos.data.face.FaceCurationLog
import eu.akoos.photos.data.face.PersonNameLog
import eu.akoos.photos.domain.catalog.CatalogMappers
import eu.akoos.photos.domain.catalog.FaceBoxRecord
import eu.akoos.photos.domain.catalog.FaceOnPhotoRecord
import eu.akoos.photos.domain.catalog.FaceRecord
import eu.akoos.photos.domain.catalog.FaceVectorRecord
import eu.akoos.photos.domain.catalog.NotPersonFaceRecord
import eu.akoos.photos.domain.catalog.NotPersonRecord
import eu.akoos.photos.domain.catalog.PeopleCatalog
import eu.akoos.photos.domain.catalog.PersonManualPhotoRecord
import eu.akoos.photos.domain.catalog.PersonRecord
import eu.akoos.photos.domain.catalog.RejectedFaceRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeopleCatalogImpl @Inject constructor(
    private val db: AppDatabase,
    private val personDao: PersonDao,
    private val faceDao: FaceDao,
    private val notPersonDao: NotPersonDao,
    private val personManualPhotoDao: PersonManualPhotoDao,
    private val personCoverDao: PersonCoverDao,
    private val faceCurationTimeDao: FaceCurationTimeDao,
    private val clusterSummaryDao: ClusterSummaryDao,
) : PeopleCatalog {
    override suspend fun <T> withTransaction(block: suspend () -> T): T = db.withTransaction(block)

    override suspend fun personById(personId: Long): PersonRecord? =
        personDao.personById(personId)?.toRecord()

    override fun observePersonForUser(userId: String, personId: Long): Flow<PersonRecord?> =
        personDao.observePersonForUser(userId, personId).map { it?.toRecord() }

    override suspend fun namedPeopleForUser(userId: String): List<PersonRecord> =
        personDao.namedPeopleForUser(userId).map { it.toRecord() }

    override fun observeNamedPeopleForUser(userId: String): Flow<List<PersonRecord>> =
        personDao.observeNamedPeopleForUser(userId).map { list -> list.map { it.toRecord() } }

    override fun observePeopleForUser(userId: String): Flow<List<PersonRecord>> =
        personDao.observePeopleForUser(userId).map { list -> list.map { it.toRecord() } }

    override fun observeFaceCountForUser(userId: String): Flow<Int> =
        faceDao.observeFaceCountForUser(userId)

    override suspend fun otherPersonId(userId: String): Long? = personDao.otherPersonId(userId)

    override suspend fun updateCoverAndCount(personId: Long, coverFaceId: String?, faceCount: Int) =
        personDao.updateCoverAndCount(personId, coverFaceId, faceCount)

    override suspend fun updateCover(personId: Long, coverFaceId: String) =
        personDao.updateCover(personId, coverFaceId)

    override suspend fun updatePersonName(personId: Long, name: String?) =
        personDao.updateName(personId, name)

    override suspend fun deleteEmptyPeople(userId: String) = personDao.deleteEmpty(userId)

    override suspend fun keptFacesForPhoto(userId: String, photoKey: String): List<FaceRecord> =
        faceDao.keptFacesForPhoto(userId, photoKey).map { it.toRecord() }

    override suspend fun facesForPerson(userId: String, personId: Long): List<FaceRecord> =
        faceDao.facesForPerson(userId, personId).map { it.toRecord() }

    override suspend fun facesOnPhotos(userId: String, photoKeys: List<String>): List<FaceOnPhotoRecord> =
        faceDao.facesOnPhotos(userId, photoKeys).map {
            FaceOnPhotoRecord(
                id = it.id,
                photoKey = it.photoKey,
                personId = it.personId,
                left = it.boxLeft,
                top = it.boxTop,
                right = it.boxRight,
                bottom = it.boxBottom,
                score = it.score,
            )
        }

    override suspend fun facePhotoKeysIn(userId: String, photoKeys: List<String>): List<String> =
        faceDao.facePhotoKeysIn(userId, photoKeys)

    override fun photoKeysForPerson(userId: String, personId: Long): Flow<List<String>> =
        faceDao.photoKeysForPerson(userId, personId)

    override fun observeFaceBoxesForPerson(userId: String, personId: Long): Flow<List<FaceBoxRecord>> =
        faceDao.observeFaceBoxesForPerson(userId, personId).map { list ->
            list.map {
                FaceBoxRecord(it.id, it.photoKey, it.boxLeft, it.boxTop, it.boxRight, it.boxBottom, it.personId, it.score)
            }
        }

    override suspend fun faceIdsForPerson(userId: String, personId: Long): List<String> =
        faceDao.faceIdsForPerson(userId, personId)

    override suspend fun faceIdsForPersonInPhotos(
        userId: String,
        personId: Long,
        photoKeys: Collection<String>,
    ): List<String> = faceDao.faceIdsForPersonInPhotos(userId, personId, photoKeys)

    override suspend fun distinctPhotoKeysForPerson(userId: String, personId: Long): List<String> =
        faceDao.distinctPhotoKeysForPerson(userId, personId)

    override suspend fun topFaceForPerson(userId: String, personId: Long): String? =
        faceDao.topFaceForPerson(userId, personId)

    override suspend fun topFaceForPersonPhoto(userId: String, personId: Long, photoKey: String): String? =
        faceDao.topFaceForPersonPhoto(userId, personId, photoKey)

    override suspend fun faceCountForPerson(userId: String, personId: Long): Int =
        faceDao.faceCountForPerson(userId, personId)

    override suspend fun upsertFaces(rows: List<FaceRecord>) {
        faceDao.upsert(rows.map { it.toEntity() })
    }

    override suspend fun labelFacesByIds(faceIds: Collection<String>, name: String) =
        faceDao.labelFacesByIds(faceIds, name)

    override suspend fun reassignFacesToPerson(faceIds: Collection<String>, personId: Long) =
        faceDao.reassignFacesToPerson(faceIds, personId)

    override suspend fun rejectFacesForPersonInPhotos(
        userId: String,
        personId: Long,
        photoKeys: Collection<String>,
    ) = faceDao.rejectFacesForPersonInPhotos(userId, personId, photoKeys)

    override suspend fun rejectedFacesForUser(userId: String): List<RejectedFaceRecord> =
        faceDao.rejectedFacesForUser(userId).map {
            RejectedFaceRecord(it.id, it.photoKey, it.boxLeft, it.boxTop, it.boxRight, it.boxBottom)
        }

    override suspend fun unrejectFace(faceId: String) = faceDao.unrejectFace(faceId)

    override suspend fun faceVectorsByIds(userId: String, faceIds: List<String>): List<FaceVectorRecord> =
        faceDao.vectorsByIdsChunk(userId, faceIds).map { FaceVectorRecord(it.id, it.embedding) }

    override suspend fun allNotPersonForUser(userId: String): List<NotPersonRecord> =
        notPersonDao.allForUser(userId).map { NotPersonRecord(it.userId, it.personName, it.faceId) }

    override suspend fun notPersonFacesForUser(userId: String): List<NotPersonFaceRecord> =
        notPersonDao.facesForUser(userId).map {
            NotPersonFaceRecord(
                it.personName, it.faceId, it.photoKey,
                it.boxLeft, it.boxTop, it.boxRight, it.boxBottom,
            )
        }

    override suspend fun addNotPersonMarks(rows: List<NotPersonRecord>) {
        notPersonDao.add(rows.map { NotPersonEntity(it.userId, it.personName, it.faceId) })
    }

    override suspend fun deleteNotPersonMark(userId: String, personName: String, faceId: String) =
        notPersonDao.deleteMark(userId, personName, faceId)

    override fun photoKeysForName(userId: String, name: String): Flow<List<String>> =
        personManualPhotoDao.photoKeysForName(userId, name)

    override suspend fun photoKeysForNameList(userId: String, name: String): List<String> =
        personManualPhotoDao.photoKeysForNameList(userId, name)

    override suspend fun addManualPhotos(rows: List<PersonManualPhotoRecord>) {
        personManualPhotoDao.add(rows.map { PersonManualPhotoEntity(it.userId, it.personName, it.photoKey) })
    }

    override suspend fun coverPhotoKeyForName(userId: String, personName: String): String? =
        personCoverDao.photoKeyForName(userId, personName)

    override suspend fun setCoverPhoto(userId: String, personName: String, photoKey: String) =
        personCoverDao.set(PersonCoverEntity(userId, personName, photoKey))

    override suspend fun clearCurationTimes(userId: String) {
        faceCurationTimeDao.clearForUser(userId)
    }

    override suspend fun noteManualPhotos(
        account: String,
        name: String,
        photoKeys: Collection<String>,
        removed: Boolean,
        at: Long,
    ) = FaceCurationLog.manualPhotos(faceCurationTimeDao, account, name, photoKeys, removed, at)

    override suspend fun noteNotPerson(
        account: String,
        name: String,
        faceIds: Collection<String>,
        removed: Boolean,
        at: Long,
    ) = FaceCurationLog.notPerson(faceCurationTimeDao, account, name, faceIds, removed, at)

    override suspend fun noteCover(account: String, name: String, removed: Boolean, at: Long) =
        FaceCurationLog.cover(faceCurationTimeDao, account, name, removed, at)

    override suspend fun notePersonNamed(account: String, name: String, at: Long) =
        PersonNameLog.named(db.personNameDao(), account, name, at)

    override suspend fun deleteClusterSummaryForPerson(personId: Long) =
        clusterSummaryDao.deleteByPerson(personId)

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
