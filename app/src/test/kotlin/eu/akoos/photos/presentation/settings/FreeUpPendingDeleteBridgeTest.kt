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
}
