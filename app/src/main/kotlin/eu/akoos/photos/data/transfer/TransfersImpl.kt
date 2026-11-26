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

package eu.akoos.photos.data.transfer

import eu.akoos.photos.di.AppScope
import eu.akoos.photos.domain.transfer.TransferActive
import eu.akoos.photos.domain.transfer.TransferHistoryEntry
import eu.akoos.photos.domain.transfer.TransferKind
import eu.akoos.photos.domain.transfer.Transfers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransfersImpl @Inject constructor(
    private val center: TransferCenter,
    @AppScope private val appScope: CoroutineScope,
) : Transfers {
    override val active: StateFlow<List<TransferActive>> = center.active
        .map { list -> list.map { it.toDomain() } }
        .stateIn(appScope, SharingStarted.Eagerly, emptyList())

    override val history: Flow<List<TransferHistoryEntry>> = center.history.map { list ->
        list.map {
            TransferHistoryEntry(it.kind, it.name, it.count, it.at, it.uris, it.savedBytes)
        }
    }

    override fun start(
        kind: TransferKind,
        total: Int,
        name: String?,
        items: List<String>,
        itemKeys: List<String>,
        itemStatus: Map<String, String>,
        onCancel: (() -> Unit)?,
        cancelFinishesRunning: Boolean,
    ): Long = center.start(
        kind = kind.toData(),
        total = total,
        name = name,
        items = items,
        itemKeys = itemKeys,
        itemStatus = itemStatus,
        onCancel = onCancel,
        cancelFinishesRunning = cancelFinishesRunning,
    )

    override fun progress(id: Long, done: Int) = center.progress(id, done)
    override fun setItemStatus(id: Long, key: String, label: String?) = center.setItemStatus(id, key, label)
    override fun finish(id: Long) = center.finish(id)
    override fun cancel(id: Long) = center.cancel(id)

    override suspend fun log(
        kind: TransferKind,
        count: Int,
        name: String?,
        uris: List<String>,
        savedBytes: Long,
    ) = center.log(kind.toData(), count, name, uris, savedBytes)

    override suspend fun clearHistory() = center.clearHistory()

    private fun TransferCenter.Active.toDomain() = TransferActive(
        id = id,
        kind = kind.toDomain(),
        done = done,
        total = total,
        name = name,
        items = items,
        cancelable = cancelable,
        itemKeys = itemKeys,
        itemStatus = itemStatus,
    )

    private fun TransferCenter.Kind.toDomain() = when (this) {
        TransferCenter.Kind.UPLOAD -> TransferKind.UPLOAD
        TransferCenter.Kind.DOWNLOAD -> TransferKind.DOWNLOAD
        TransferCenter.Kind.OFFLINE -> TransferKind.OFFLINE
    }

    private fun TransferKind.toData() = when (this) {
        TransferKind.UPLOAD -> TransferCenter.Kind.UPLOAD
        TransferKind.DOWNLOAD -> TransferCenter.Kind.DOWNLOAD
        TransferKind.OFFLINE -> TransferCenter.Kind.OFFLINE
    }
}
