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

package eu.akoos.photos.data.catalog

import eu.akoos.photos.data.repository.SyncStateSnapshot
import eu.akoos.photos.domain.catalog.SyncSnapshot
import eu.akoos.photos.domain.catalog.SyncSnapshotRows
import eu.akoos.photos.domain.catalog.SyncStateLiteRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncSnapshotImpl @Inject constructor(
    private val snapshot: SyncStateSnapshot,
) : SyncSnapshot {
    override fun observe(userId: UserId): Flow<SyncSnapshotRows> =
        snapshot.observe(userId).map { it.toDomain() }

    override suspend fun current(userId: UserId): SyncSnapshotRows =
        snapshot.current(userId).toDomain()

    private fun SyncStateSnapshot.Rows.toDomain() = SyncSnapshotRows(
        rows = rows.map {
            SyncStateLiteRecord(it.localUri, it.cloudFileId, it.status.name)
        },
    )
}
