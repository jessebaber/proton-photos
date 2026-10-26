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

package eu.akoos.photos.presentation.settings

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeUpPendingDeleteBridgeTest {

    @Test
    fun settle_granted_removes_uris_and_emits_matching_settlement() = runTest {
        val bridge = FreeUpPendingDeleteBridge()
        bridge.offer(listOf("content://a", "content://b"))

        // UNDISPATCHED so collect is subscribed before settle's tryEmit.
        val settlement = async(start = CoroutineStart.UNDISPATCHED) { bridge.settled.first() }
        bridge.settle(setOf("content://a"), granted = true)

        assertEquals(
            FreeUpConsentSettlement(uris = setOf("content://a"), granted = true),
            settlement.await(),
        )
        assertEquals(setOf("content://b"), bridge.uris.value)
    }

    @Test
    fun settle_denied_removes_uris_and_emits_granted_false() = runTest {
        val bridge = FreeUpPendingDeleteBridge()
        bridge.offer(listOf("content://a", "content://b"))

        val settlement = async(start = CoroutineStart.UNDISPATCHED) { bridge.settled.first() }
        bridge.settle(setOf("content://a", "content://b"), granted = false)

        assertEquals(
            FreeUpConsentSettlement(uris = setOf("content://a", "content://b"), granted = false),
            settlement.await(),
        )
        assertTrue(bridge.uris.value.isEmpty())
    }

    @Test
    fun settle_empty_uris_is_noop_with_no_emit() = runTest {
        val bridge = FreeUpPendingDeleteBridge()
        bridge.offer(listOf("content://a"))

        val emissions = mutableListOf<FreeUpConsentSettlement>()
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            bridge.settled.collect { emissions.add(it) }
        }

        bridge.settle(emptySet(), granted = true)

        assertTrue(emissions.isEmpty())
        assertEquals(setOf("content://a"), bridge.uris.value)
    }

    @Test
    fun settleAllPending_denied_drains_queue_so_no_uris_remain_to_reoffer() = runTest {
        val bridge = FreeUpPendingDeleteBridge()
        bridge.offer(listOf("content://chunk1-a", "content://chunk1-b", "content://chunk2-a"))

        val settlement = async(start = CoroutineStart.UNDISPATCHED) { bridge.settled.first() }
        // Deny path: settle the entire queue, not only the dialog batch.
        bridge.settleAllPending(granted = false)

        assertEquals(
            FreeUpConsentSettlement(
                uris = setOf("content://chunk1-a", "content://chunk1-b", "content://chunk2-a"),
                granted = false,
            ),
            settlement.await(),
        )
        assertTrue(bridge.uris.value.isEmpty())
    }

    @Test
    fun grant_chunk_then_remaining_stay_pending_for_next_offer() = runTest {
        val bridge = FreeUpPendingDeleteBridge()
        bridge.offer(listOf("content://a", "content://b", "content://c"))

        val settlement = async(start = CoroutineStart.UNDISPATCHED) { bridge.settled.first() }
        bridge.settle(setOf("content://a", "content://b"), granted = true)

        assertEquals(
            FreeUpConsentSettlement(uris = setOf("content://a", "content://b"), granted = true),
            settlement.await(),
        )
        assertEquals(setOf("content://c"), bridge.uris.value)
    }
}
