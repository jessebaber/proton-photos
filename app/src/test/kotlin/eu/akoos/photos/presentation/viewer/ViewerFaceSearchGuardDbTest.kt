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

package eu.akoos.photos.presentation.viewer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import eu.akoos.photos.data.db.AppDatabase
import eu.akoos.photos.data.db.entity.FaceEntity
import eu.akoos.photos.data.db.entity.FaceScanEntity
import eu.akoos.photos.data.db.entity.PersonEntity
import eu.akoos.photos.domain.catalog.PeopleCatalog
import eu.akoos.photos.domain.catalog.FaceBoxRecord
import eu.akoos.photos.domain.catalog.FaceRecord
import eu.akoos.photos.domain.catalog.NotPersonRecord
import eu.akoos.photos.domain.catalog.PersonRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The viewer's on-demand face search must never re-detect a photo whose faces the user already curated:
 * a removed face hides from the "people in this photo" bar and one left ungrouped shows there as a face of
 * its own, and either way the photo holds face rows, so a forced scan would overwrite them. Only a photo with no face row at all may be searched.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ViewerFaceSearchGuardDbTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun people(): PeopleCatalog = object : PeopleCatalog {
        override suspend fun personById(personId: Long): PersonRecord? = null
        override fun observePersonForUser(userId: String, personId: Long) = flowOf(null)
        override suspend fun namedPeopleForUser(userId: String) = emptyList<PersonRecord>()
        override suspend fun otherPersonId(userId: String): Long? = null
        override suspend fun updateCoverAndCount(personId: Long, coverFaceId: String?, faceCount: Int) {}
        override suspend fun keptFacesForPhoto(userId: String, photoKey: String) = emptyList<FaceRecord>()
        override suspend fun facesForPerson(userId: String, personId: Long) = emptyList<FaceRecord>()
        override suspend fun facesOnPhotos(userId: String, photoKeys: List<String>) = emptyList<FaceBoxRecord>()
        override suspend fun facePhotoKeysIn(userId: String, photoKeys: List<String>): List<String> =
            db.faceDao().facePhotoKeysIn(userId, photoKeys)
        override fun photoKeysForPerson(userId: String, personId: Long) = flowOf(emptyList<String>())
        override fun observeFaceBoxesForPerson(userId: String, personId: Long) = flowOf(emptyList<FaceBoxRecord>())
        override suspend fun faceIdsForPerson(userId: String, personId: Long) = emptyList<String>()
        override suspend fun distinctPhotoKeysForPerson(userId: String, personId: Long) = emptyList<String>()
        override suspend fun topFaceForPerson(userId: String, personId: Long): String? = null
        override suspend fun faceCountForPerson(userId: String, personId: Long) = 0
        override suspend fun upsertFaces(rows: List<FaceRecord>) {}
        override suspend fun allNotPersonForUser(userId: String) = emptyList<NotPersonRecord>()
        override fun photoKeysForName(userId: String, name: String) = flowOf(emptyList<String>())
        override suspend fun photoKeysForNameList(userId: String, name: String) = emptyList<String>()
        override suspend fun clearCurationTimes(userId: String) {}
    }

    private fun face(user: String, photoKey: String, personId: Long?, rejected: Boolean = false) = FaceEntity(
        id = "$photoKey#0", userId = user, photoKey = photoKey,
        left = 0f, top = 0f, right = 1f, bottom = 1f, landmarks = "",
        embedding = ByteArray(0), personId = personId, score = 0.9f, rejected = rejected,
    )

    @Test
    fun `a removed face blocks the search`() = runTest {
        db.faceDao().upsert(face(USER, PHOTO, personId = null, rejected = true))

        assertTrue(db.faceDao().keptFacesForPhoto(USER, PHOTO).isEmpty())
        assertFalse(viewerMayDetectFaces(people(), USER, PHOTO))
    }

    @Test
    fun `an ungrouped face blocks the search and still shows`() = runTest {
        db.faceDao().upsert(face(USER, PHOTO, personId = null))

        // A face just taken off a person is ungrouped, and the bar lists it so it can be named again.
        assertTrue(db.faceDao().keptFacesForPhoto(USER, PHOTO).single().personId == null)
        assertTrue(isLoneFace(personId = null, otherId = 9L))
        assertFalse(viewerMayDetectFaces(people(), USER, PHOTO))
    }

    @Test
    fun `a grouped face blocks the search`() = runTest {
        db.personDao().upsert(PersonEntity(id = 3L, userId = USER, displayName = "Alice", faceCount = 1))
        db.faceDao().upsert(face(USER, PHOTO, personId = 3L))

        assertFalse(viewerMayDetectFaces(people(), USER, PHOTO))
    }

    @Test
    fun `a scanned photo with no faces may be searched`() = runTest {
        db.faceScanDao().upsert(listOf(FaceScanEntity(USER, PHOTO)))

        assertTrue(viewerMayDetectFaces(people(), USER, PHOTO))
    }

    @Test
    fun `another account's face does not block the search`() = runTest {
        db.faceDao().upsert(face("other", PHOTO, personId = null, rejected = true))

        assertTrue(viewerMayDetectFaces(people(), USER, PHOTO))
        assertFalse(viewerMayDetectFaces(people(), "other", PHOTO))
    }

    @Test
    fun `a guest's removed face blocks the guest's search`() = runTest {
        db.faceDao().upsert(face(GUEST, PHOTO, personId = null, rejected = true))

        assertFalse(viewerMayDetectFaces(people(), GUEST, PHOTO))
    }

    @Test
    fun `a grouped face is a person chip, an Unsorted or unplaced one a lone face`() {
        assertFalse(isLoneFace(personId = 3L, otherId = 9L))
        assertTrue(isLoneFace(personId = 9L, otherId = 9L))
        assertTrue(isLoneFace(personId = null, otherId = null))
    }

    private companion object {
        const val USER = "acct"
        const val GUEST = "local"
        const val PHOTO = "content://media/external/images/media/7"
    }
}
