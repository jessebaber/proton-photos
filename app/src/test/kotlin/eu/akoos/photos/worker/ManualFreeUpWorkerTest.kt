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

package eu.akoos.photos.worker

import androidx.work.NetworkType
import eu.akoos.photos.data.notification.NotificationIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins ManualFreeUpWorker companion contracts that do not need Robolectric: CONNECTED constraint
 * and a unique notification id outside the album-download range.
 */
class ManualFreeUpWorkerTest {

    @Test
    fun `verify constraints require a connected network`() {
        val constraints = ManualFreeUpWorker.verifyConstraints()
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
    }

    @Test
    fun `manual free-up notification id is unique and outside the album range`() {
        assertTrue(NotificationIds.MANUAL_FREE_UP !in NotificationIds.ALBUM_DOWNLOAD_RANGE)
        assertTrue(NotificationIds.MANUAL_FREE_UP_READY !in NotificationIds.ALBUM_DOWNLOAD_RANGE)
        assertTrue(NotificationIds.AUTO_FREE_UP !in NotificationIds.ALBUM_DOWNLOAD_RANGE)
        assertNotEquals(NotificationIds.MANUAL_FREE_UP, NotificationIds.MANUAL_FREE_UP_READY)
        assertNotEquals(NotificationIds.MANUAL_FREE_UP, NotificationIds.AUTO_FREE_UP)
        assertNotEquals(NotificationIds.MANUAL_FREE_UP, NotificationIds.DELETE_CONSENT)
        assertNotEquals(NotificationIds.MANUAL_FREE_UP, NotificationIds.METADATA_EDIT)
    }

    @Test
    fun `AUTO_FREE_UP and thermal gates do not appear on the manual companion surface`() {
        // Manual free-up must not reuse FreeUpSpaceWorker's skip helpers: uniqueness of the
        // unique name + CONNECTED-only constraints is the regression lock here.
        assertEquals("manual_free_up", ManualFreeUpWorker.UNIQUE_NAME)
        assertEquals("manual_free_up_worker", ManualFreeUpWorker.TAG)
        assertEquals(FreeUpPipelineWorker.manualConstraints(), ManualFreeUpWorker.verifyConstraints())
    }

    @Test
    fun `manual enqueue targets the shared pipeline with a distinct unique name`() {
        assertTrue(
            FreeUpPipelineWorker.runsFullReclaim(FreeUpPipelineWorker.TRIGGER_MANUAL),
        )
        assertNotEquals(ManualFreeUpWorker.UNIQUE_NAME, FreeUpSpaceWorker.TAG)
    }
}
