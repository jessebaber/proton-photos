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
import eu.akoos.photos.domain.usecase.FreeUpSpaceUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins FreeUpPipelineWorker companion contracts: unique-name isolation, progress keys, and that
 * automatic work requests full reclaim (not verify-only).
 */
class FreeUpPipelineWorkerTest {

    @Test
    fun `manual and automatic unique names stay distinct`() {
        assertEquals("manual_free_up", ManualFreeUpWorker.UNIQUE_NAME)
        assertEquals("free_up_worker", FreeUpSpaceWorker.TAG)
        assertEquals("free_up_worker_oneshot", FreeUpSpaceWorker.NAME_ONESHOT)
        assertNotEquals(ManualFreeUpWorker.UNIQUE_NAME, FreeUpSpaceWorker.TAG)
        assertNotEquals(ManualFreeUpWorker.UNIQUE_NAME, FreeUpSpaceWorker.NAME_ONESHOT)
    }

    @Test
    fun `manual constraints require network only`() {
        val constraints = FreeUpPipelineWorker.manualConstraints()
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
        assertFalse(constraints.requiresBatteryNotLow())
    }

    @Test
    fun `automatic constraints match the sweep gate set`() {
        val constraints = FreeUpPipelineWorker.automaticConstraints()
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
        assertTrue(constraints.requiresBatteryNotLow())
        assertEquals(FreeUpSpaceWorker.sweepConstraints().requiredNetworkType, constraints.requiredNetworkType)
    }

    @Test
    fun `progress key contract is phase plus counts only`() {
        assertEquals("phase", FreeUpPipelineWorker.KEY_PHASE)
        assertEquals("processed", FreeUpPipelineWorker.KEY_PROCESSED)
        assertEquals("total", FreeUpPipelineWorker.KEY_TOTAL)
        assertEquals("removed", FreeUpPipelineWorker.KEY_REMOVED)
        assertEquals("needs_permission_count", FreeUpPipelineWorker.KEY_NEEDS_PERMISSION_COUNT)
        assertEquals("verifying", FreeUpPipelineWorker.PHASE_VERIFYING)
        assertEquals("rechecking", FreeUpPipelineWorker.PHASE_RECHECKING)
        assertEquals("deleting", FreeUpPipelineWorker.PHASE_DELETING)
        assertEquals("awaiting_consent", FreeUpPipelineWorker.PHASE_AWAITING_CONSENT)
    }

    @Test
    fun `progress data never embeds URI lists`() {
        val data = FreeUpPipelineWorker.progressData(
            phase = FreeUpPipelineWorker.PHASE_DELETING,
            processed = 3,
            total = 10,
            removed = 2,
            needsPermissionCount = 1,
        )
        assertEquals(FreeUpPipelineWorker.PHASE_DELETING, data.getString(FreeUpPipelineWorker.KEY_PHASE))
        assertEquals(3, data.getInt(FreeUpPipelineWorker.KEY_PROCESSED, -1))
        assertEquals(10, data.getInt(FreeUpPipelineWorker.KEY_TOTAL, -1))
        assertEquals(2, data.getInt(FreeUpPipelineWorker.KEY_REMOVED, -1))
        assertEquals(1, data.getInt(FreeUpPipelineWorker.KEY_NEEDS_PERMISSION_COUNT, -1))
        assertEquals(3, data.getInt(FreeUpPipelineWorker.KEY_DONE, -1))
        assertFalse(data.keyValueMap.keys.any { it.contains("uri", ignoreCase = true) })
    }

    @Test
    fun `automatic trigger requests full reclaim not verify-only`() {
        val data = FreeUpPipelineWorker.automaticInputData(86_400_000L)
        assertEquals(
            FreeUpPipelineWorker.TRIGGER_AUTOMATIC,
            data.getString(FreeUpPipelineWorker.KEY_TRIGGER),
        )
        assertTrue(
            "automatic free-up must run verify+recheck+reclaim",
            FreeUpPipelineWorker.runsFullReclaim(FreeUpPipelineWorker.TRIGGER_AUTOMATIC),
        )
        assertTrue(FreeUpPipelineWorker.protectDownloadedFor(FreeUpPipelineWorker.TRIGGER_AUTOMATIC))
        assertFalse(FreeUpPipelineWorker.protectDownloadedFor(FreeUpPipelineWorker.TRIGGER_MANUAL))
    }

    @Test
    fun `manual trigger uses max olderThan and does not protect downloads`() {
        val now = 1_750_000_000_000L
        assertEquals(
            Long.MAX_VALUE,
            FreeUpPipelineWorker.olderThanMsFor(FreeUpPipelineWorker.TRIGGER_MANUAL, now, 1L),
        )
        assertEquals(
            now - 86_400_000L,
            FreeUpPipelineWorker.olderThanMsFor(
                FreeUpPipelineWorker.TRIGGER_AUTOMATIC,
                now,
                86_400_000L,
            ),
        )
        assertTrue(FreeUpPipelineWorker.runsFullReclaim(FreeUpPipelineWorker.TRIGGER_MANUAL))
    }

    @Test
    fun `pipeline phases map to WorkInfo wire names`() {
        assertEquals(
            FreeUpPipelineWorker.PHASE_VERIFYING,
            FreeUpPipelineWorker.phaseWireName(FreeUpSpaceUseCase.PipelinePhase.Verifying),
        )
        assertEquals(
            FreeUpPipelineWorker.PHASE_RECHECKING,
            FreeUpPipelineWorker.phaseWireName(FreeUpSpaceUseCase.PipelinePhase.Rechecking),
        )
        assertEquals(
            FreeUpPipelineWorker.PHASE_DELETING,
            FreeUpPipelineWorker.phaseWireName(FreeUpSpaceUseCase.PipelinePhase.Deleting),
        )
    }

    @Test
    fun `bridge offer is gated on process UI presence`() {
        assertTrue(FreeUpPipelineWorker.shouldOfferBridge(processUiPresent = true))
        assertFalse(FreeUpPipelineWorker.shouldOfferBridge(processUiPresent = false))
    }

    @Test
    fun `manual and auto foreground notification ids stay distinct`() {
        assertEquals(
            NotificationIds.MANUAL_FREE_UP,
            FreeUpPipelineWorker.foregroundNotificationId(isManual = true),
        )
        assertEquals(
            NotificationIds.AUTO_FREE_UP,
            FreeUpPipelineWorker.foregroundNotificationId(isManual = false),
        )
        assertNotEquals(NotificationIds.MANUAL_FREE_UP, NotificationIds.AUTO_FREE_UP)
        assertTrue(NotificationIds.AUTO_FREE_UP !in NotificationIds.ALBUM_DOWNLOAD_RANGE)
    }
}
