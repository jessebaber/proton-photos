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
import me.proton.core.domain.entity.UserId

/** Lean sync_state projection shared by gallery/viewer. */
data class SyncSnapshotRows(
    val rows: List<SyncStateLiteRecord>,
) {
    val hiddenCloudIds: Set<String> by lazy {
        rows.asSequence().filter { it.status == "HIDDEN" }.mapNotNull { it.cloudFileId }.toSet()
    }
    val syncedCloudIds: Set<String> by lazy {
        rows.asSequence().filter { it.status == "SYNCED" }.mapNotNull { it.cloudFileId }.toSet()
    }
}

/** Live sync_state snapshot presentation uses instead of SyncStateSnapshot. */
interface SyncSnapshot {
    fun observe(userId: UserId): Flow<SyncSnapshotRows>
    suspend fun current(userId: UserId): SyncSnapshotRows
}
