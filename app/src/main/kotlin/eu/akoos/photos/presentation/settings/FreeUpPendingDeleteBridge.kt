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

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** Outcome of a MediaStore permanent-delete consent dialog for free-up reclaim. */
data class FreeUpConsentSettlement(
    val uris: Set<String>,
    val granted: Boolean,
)

/**
 * In-memory permanent-delete URI queue for free-up reclaim. Hard-separated from
 * [eu.akoos.photos.data.preferences.SettingsKeys.PENDING_DELETE_URIS] (trash / delete-after-backup).
 *
 * Process death clears this queue: verified markers plus the ready / awaiting-consent notification
 * reopen Free-up; the bridge is best-effort only while the process is alive.
 */
@Singleton
class FreeUpPendingDeleteBridge @Inject constructor() {
    private val _uris = MutableStateFlow<Set<String>>(emptySet())
    val uris: StateFlow<Set<String>> = _uris.asStateFlow()

    private val _settled = MutableSharedFlow<FreeUpConsentSettlement>(
        replay = 0,
        // Multi-chunk grants settle sequentially; keep a small buffer so a slow collector does not
        // drop an intermediate chunk settlement.
        extraBufferCapacity = 8,
    )
    val settled: SharedFlow<FreeUpConsentSettlement> = _settled.asSharedFlow()

    fun offer(uris: List<String>) {
        if (uris.isEmpty()) return
        _uris.update { it + uris }
    }

    /**
     * Removes [uris] from the queue and emits one [FreeUpConsentSettlement].
     * Empty [uris] is a no-op (no emit).
     */
    fun settle(uris: Set<String>, granted: Boolean) {
        if (uris.isEmpty()) return
        _uris.update { it - uris }
        _settled.tryEmit(FreeUpConsentSettlement(uris = uris, granted = granted))
    }

    /**
     * Settles every URI currently in the queue with [granted]. Used when the user denies a consent
     * chunk so remaining offered URIs are not prompted again in this session.
     */
    fun settleAllPending(granted: Boolean) {
        val snapshot = _uris.value
        if (snapshot.isEmpty()) return
        settle(snapshot, granted)
    }

    fun remove(uris: Set<String>) {
        if (uris.isEmpty()) return
        _uris.update { it - uris }
    }
}
