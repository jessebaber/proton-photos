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
    suspend fun personById(personId: Long): PersonRecord?
    fun observePersonForUser(userId: String, personId: Long): Flow<PersonRecord?>
    suspend fun namedPeopleForUser(userId: String): List<PersonRecord>
    fun observeNamedPeopleForUser(userId: String): Flow<List<PersonRecord>>
    fun observePeopleForUser(userId: String): Flow<List<PersonRecord>>
    fun observeFaceCountForUser(userId: String): Flow<Int>
    suspend fun otherPersonId(userId: String): Long?
    suspend fun updateCoverAndCount(personId: Long, coverFaceId: String?, faceCount: Int)

    suspend fun keptFacesForPhoto(userId: String, photoKey: String): List<FaceRecord>
    suspend fun facesForPerson(userId: String, personId: Long): List<FaceRecord>
    suspend fun facesOnPhotos(userId: String, photoKeys: List<String>): List<FaceBoxRecord>
    suspend fun facePhotoKeysIn(userId: String, photoKeys: List<String>): List<String>
    fun photoKeysForPerson(userId: String, personId: Long): Flow<List<String>>
    fun observeFaceBoxesForPerson(userId: String, personId: Long): Flow<List<FaceBoxRecord>>
    suspend fun faceIdsForPerson(userId: String, personId: Long): List<String>
    suspend fun distinctPhotoKeysForPerson(userId: String, personId: Long): List<String>
    suspend fun topFaceForPerson(userId: String, personId: Long): String?
    suspend fun faceCountForPerson(userId: String, personId: Long): Int
    suspend fun upsertFaces(rows: List<FaceRecord>)

    suspend fun allNotPersonForUser(userId: String): List<NotPersonRecord>
    fun photoKeysForName(userId: String, name: String): Flow<List<String>>
    suspend fun photoKeysForNameList(userId: String, name: String): List<String>

    suspend fun clearCurationTimes(userId: String)
}
