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

import eu.akoos.photos.domain.entity.SyncState
import eu.akoos.photos.domain.entity.SyncStatus
import me.proton.core.domain.entity.UserId

sealed interface FreeUpResult<out Consent> {
    /** [skipped] counts device copies this app could not delete itself on Android 10 and older. */
    data class Done(val freed: Int, val skipped: Int = 0) : FreeUpResult<Nothing>

    data class NeedsPermission<Consent>(
        val pendingIntent: Consent,
        val localUris: List<String>,
        val remaining: List<SyncState> = emptyList(),
    ) : FreeUpResult<Consent>
}

/**
 * Reclaims device copies of photos already backed up to Drive. [C] is the platform consent payload
 * ([android.app.PendingIntent] in the Android binding).
 */
interface FreeUpSpaceUseCase<C> {
    suspend fun candidates(
        userId: UserId,
        olderThanMs: Long,
        protectDownloaded: Boolean = false,
    ): List<SyncState>

    suspend fun verifyActiveBackups(
        userId: UserId,
        candidates: List<SyncState>,
    ): List<SyncState>

    suspend operator fun invoke(
        userId: UserId,
        olderThanMs: Long,
        protectDownloaded: Boolean = false,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): FreeUpResult<C>

    suspend fun sweepConfirmed(
        userId: UserId,
        olderThanMs: Long,
        protectDownloaded: Boolean = false,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): FreeUpResult<C>

    suspend fun verifyAndReclaim(
        userId: UserId,
        candidates: List<SyncState>,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): FreeUpResult<C>

    suspend fun consentFor(userId: UserId, rows: List<SyncState>, freed: Int = 0): FreeUpResult<C>

    suspend fun reclaimCandidates(
        candidates: List<SyncState>,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): FreeUpResult<C>

    companion object {
        const val VERIFY_CHUNK = 50
        const val DELETE_REQUEST_MAX_URIS = 2_000

        fun <T> splitForConsent(rows: List<T>, max: Int = DELETE_REQUEST_MAX_URIS): Pair<List<T>, List<T>> =
            rows.take(max) to rows.drop(max)

        fun isEligibleForReclamation(
            state: SyncState,
            olderThanMs: Long,
            protectDownloaded: Boolean = false,
        ): Boolean {
            val backedUpAtMs = state.backedUpAtMs
            return state.status == SyncStatus.SYNCED &&
                backedUpAtMs != null &&
                backedUpAtMs < olderThanMs &&
                (!protectDownloaded || state.localHash.isNotEmpty())
        }
    }
}
