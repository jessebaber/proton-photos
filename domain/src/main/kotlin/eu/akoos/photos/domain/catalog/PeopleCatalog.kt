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

/** People / face / not-person / cover / manual-photo read-write catalog. */
interface PeopleCatalog {
    suspend fun <T> withTransaction(block: suspend () -> T): T

    suspend fun personById(personId: Long): PersonRecord?
    fun observePersonForUser(userId: String, personId: Long): Flow<PersonRecord?>
    suspend fun namedPeopleForUser(userId: String): List<PersonRecord>
    fun observeNamedPeopleForUser(userId: String): Flow<List<PersonRecord>>
    fun observePeopleForUser(userId: String): Flow<List<PersonRecord>>
    fun observeFaceCountForUser(userId: String): Flow<Int>
    suspend fun otherPersonId(userId: String): Long?
    suspend fun updateCoverAndCount(personId: Long, coverFaceId: String?, faceCount: Int)
    suspend fun updateCover(personId: Long, coverFaceId: String)
    suspend fun updatePersonName(personId: Long, name: String?)
    suspend fun deleteEmptyPeople(userId: String)

    suspend fun keptFacesForPhoto(userId: String, photoKey: String): List<FaceRecord>
    suspend fun facesForPerson(userId: String, personId: Long): List<FaceRecord>
    suspend fun facesOnPhotos(userId: String, photoKeys: List<String>): List<FaceOnPhotoRecord>
    suspend fun facePhotoKeysIn(userId: String, photoKeys: List<String>): List<String>
    fun photoKeysForPerson(userId: String, personId: Long): Flow<List<String>>
    fun observeFaceBoxesForPerson(userId: String, personId: Long): Flow<List<FaceBoxRecord>>
    suspend fun faceIdsForPerson(userId: String, personId: Long): List<String>
    suspend fun faceIdsForPersonInPhotos(userId: String, personId: Long, photoKeys: Collection<String>): List<String>
    suspend fun distinctPhotoKeysForPerson(userId: String, personId: Long): List<String>
    suspend fun topFaceForPerson(userId: String, personId: Long): String?
    suspend fun topFaceForPersonPhoto(userId: String, personId: Long, photoKey: String): String?
    suspend fun faceCountForPerson(userId: String, personId: Long): Int
    suspend fun upsertFaces(rows: List<FaceRecord>)
    suspend fun labelFacesByIds(faceIds: Collection<String>, name: String)
    suspend fun reassignFacesToPerson(faceIds: Collection<String>, personId: Long)
    suspend fun rejectFacesForPersonInPhotos(userId: String, personId: Long, photoKeys: Collection<String>)
    suspend fun rejectedFacesForUser(userId: String): List<RejectedFaceRecord>
    suspend fun unrejectFace(faceId: String)
    suspend fun faceVectorsByIds(userId: String, faceIds: List<String>): List<FaceVectorRecord>

    suspend fun allNotPersonForUser(userId: String): List<NotPersonRecord>
    suspend fun notPersonFacesForUser(userId: String): List<NotPersonFaceRecord>
    suspend fun addNotPersonMarks(rows: List<NotPersonRecord>)
    suspend fun deleteNotPersonMark(userId: String, personName: String, faceId: String)

    fun photoKeysForName(userId: String, name: String): Flow<List<String>>
    suspend fun photoKeysForNameList(userId: String, name: String): List<String>
    suspend fun addManualPhotos(rows: List<PersonManualPhotoRecord>)

    suspend fun coverPhotoKeyForName(userId: String, personName: String): String?
    suspend fun setCoverPhoto(userId: String, personName: String, photoKey: String)

    suspend fun clearCurationTimes(userId: String)
    suspend fun noteManualPhotos(account: String, name: String, photoKeys: Collection<String>, removed: Boolean, at: Long)
    suspend fun noteNotPerson(account: String, name: String, faceIds: Collection<String>, removed: Boolean, at: Long)
    suspend fun noteCover(account: String, name: String, removed: Boolean, at: Long)
    suspend fun notePersonNamed(account: String, name: String, at: Long)

    suspend fun deleteClusterSummaryForPerson(personId: Long)
}
