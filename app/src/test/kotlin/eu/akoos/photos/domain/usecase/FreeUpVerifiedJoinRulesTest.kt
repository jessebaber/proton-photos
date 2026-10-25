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

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the `free_up_verified` ↔ `sync_state` join gates before the table lands: ready-to-reclaim
 * requires matching [userId], matching [cloudFileId], SYNCED status, and a fresh [verifiedAtMs].
 */
class FreeUpVerifiedJoinRulesTest {

    private val now = 1_000_000L
    private val since = now - 24 * 60 * 60 * 1000L

    @Test
    fun `matching synced fresh row is ready`() {
        assertTrue(
            matchesFreeUpVerifiedJoin(
                syncUserId = "u1",
                queryUserId = "u1",
                syncStatus = "SYNCED",
                syncCloudFileId = "cloud-1",
                verifiedUserId = "u1",
                verifiedCloudFileId = "cloud-1",
                verifiedAtMs = now,
                sinceMs = since,
            ),
        )
    }

    @Test
    fun `other userId is excluded`() {
        assertFalse(
            matchesFreeUpVerifiedJoin(
                syncUserId = "u2",
                queryUserId = "u1",
                syncStatus = "SYNCED",
                syncCloudFileId = "cloud-1",
                verifiedUserId = "u2",
                verifiedCloudFileId = "cloud-1",
                verifiedAtMs = now,
                sinceMs = since,
            ),
        )
    }

    @Test
    fun `repaired cloudFileId is excluded`() {
        assertFalse(
            matchesFreeUpVerifiedJoin(
                syncUserId = "u1",
                queryUserId = "u1",
                syncStatus = "SYNCED",
                syncCloudFileId = "cloud-new",
                verifiedUserId = "u1",
                verifiedCloudFileId = "cloud-old",
                verifiedAtMs = now,
                sinceMs = since,
            ),
        )
    }

    @Test
    fun `expired verify is excluded`() {
        assertFalse(
            matchesFreeUpVerifiedJoin(
                syncUserId = "u1",
                queryUserId = "u1",
                syncStatus = "SYNCED",
                syncCloudFileId = "cloud-1",
                verifiedUserId = "u1",
                verifiedCloudFileId = "cloud-1",
                verifiedAtMs = since - 1L,
                sinceMs = since,
            ),
        )
    }

    @Test
    fun `non SYNCED status is excluded`() {
        assertFalse(
            matchesFreeUpVerifiedJoin(
                syncUserId = "u1",
                queryUserId = "u1",
                syncStatus = "CLOUD_ONLY",
                syncCloudFileId = "cloud-1",
                verifiedUserId = "u1",
                verifiedCloudFileId = "cloud-1",
                verifiedAtMs = now,
                sinceMs = since,
            ),
        )
    }
}
