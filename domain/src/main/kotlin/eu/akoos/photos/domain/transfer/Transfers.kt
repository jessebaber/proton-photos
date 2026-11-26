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

package eu.akoos.photos.domain.transfer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/** Kind of live or logged transfer the activity screen shows. */
enum class TransferKind { UPLOAD, DOWNLOAD, OFFLINE }

/** A transfer running right now that is not a WorkManager job. */
data class TransferActive(
    val id: Long,
    val kind: TransferKind,
    val done: Int,
    val total: Int,
    val name: String? = null,
    val items: List<String> = emptyList(),
    val cancelable: Boolean = false,
    val itemKeys: List<String> = emptyList(),
    val itemStatus: Map<String, String> = emptyMap(),
)

/** One finished transfer for the History tab. [kind] is the [TransferKind] name. */
@Serializable
data class TransferHistoryEntry(
    val kind: String,
    val name: String? = null,
    val count: Int,
    val at: Long,
    val uris: List<String> = emptyList(),
    val savedBytes: Long = 0L,
)

/**
 * Live transfer tracking and the persisted history log the activity screen reads.
 * Implementations hold any [Context] they need; callers never pass Android types.
 */
interface Transfers {
    val active: StateFlow<List<TransferActive>>
    val history: Flow<List<TransferHistoryEntry>>

    fun start(
        kind: TransferKind,
        total: Int,
        name: String? = null,
        items: List<String> = emptyList(),
        itemKeys: List<String> = emptyList(),
        itemStatus: Map<String, String> = emptyMap(),
        onCancel: (() -> Unit)? = null,
        cancelFinishesRunning: Boolean = false,
    ): Long

    fun progress(id: Long, done: Int)
    fun setItemStatus(id: Long, key: String, label: String?)
    fun finish(id: Long)
    fun cancel(id: Long)
    suspend fun log(
        kind: TransferKind,
        count: Int,
        name: String? = null,
        uris: List<String> = emptyList(),
        savedBytes: Long = 0L,
    )
    suspend fun clearHistory()
}
