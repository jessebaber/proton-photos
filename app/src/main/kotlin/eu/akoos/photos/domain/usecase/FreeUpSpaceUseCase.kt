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

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.akoos.photos.data.db.dao.FreeUpVerifiedDao
import eu.akoos.photos.data.db.entity.FreeUpVerifiedEntity
import eu.akoos.photos.data.repository.drive.LinkDetailHelpers
import eu.akoos.photos.domain.entity.SyncState
import eu.akoos.photos.domain.entity.SyncStatus
import eu.akoos.photos.domain.repository.DrivePhotoRepository
import eu.akoos.photos.domain.repository.SyncStateRepository
import eu.akoos.photos.util.isOfflineError
import eu.akoos.photos.util.isTransientApiError
import eu.akoos.photos.util.retryWithBackoff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Free-up candidates, chunked Drive verify into `free_up_verified`, and foreground reclaim.
 *
 * A [Mutex] serialises verify/reclaim so manual and scheduled paths cannot process the same rows
 * concurrently: manual waits; scheduled (PR3) uses [tryLock] and skips when busy.
 */
@Singleton
class FreeUpSpaceUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncStateRepo: SyncStateRepository,
    private val cloudRepo: DrivePhotoRepository,
    private val linkDetailHelpers: LinkDetailHelpers,
    private val freeUpVerifiedDao: FreeUpVerifiedDao,
) {
    private val mutex = Mutex()

    sealed class FreeUpResult {
        data class Done(val freed: Int) : FreeUpResult()
        /** Android 11+: system delete dialog must be shown for these URIs. */
        data class NeedsPermission(
            val pendingIntent: PendingIntent,
            val localUris: List<String>,
        ) : FreeUpResult()
        /**
         * Consent-request construction failed after [freed] direct deletes already landed. Surfaced
         * instead of silently reporting [Done] with a leftover NeedsPermission intent the user never
         * sees.
         */
        data class Failed(val message: String, val freed: Int = 0) : FreeUpResult()
    }

    /**
     * Exactly the rows [invoke] would act on, for a caller that has to show the user what is about to
     * leave their device before it does. Sharing one query is the point: a list drawn from a second,
     * similar-looking filter could name a photo the sweep spares, or spare one the sweep takes, and
     * the screen would be a promise about a different set than the one that runs.
     *
     * Every row is `SYNCED`, so every photo in it has a Drive copy. That is what makes the backed-up
     * badge on each cell a check the user can make rather than a decoration.
     */
    suspend fun candidates(
        userId: UserId,
        olderThanMs: Long,
        protectDownloaded: Boolean = false,
    ): List<SyncState> = withContext(Dispatchers.IO) {
        syncStateRepo.getSyncedBefore(userId, olderThanMs)
            .filter { isEligibleForReclamation(it, olderThanMs, protectDownloaded) }
    }

    /**
     * Chunked Drive verify that persists successes into `free_up_verified`. Skips candidates already
     * verified within [VERIFY_FRESHNESS_MS]. Each chunk uses the strict link fetch + [retryWithBackoff]
     * (including offline); a failed chunk never writes ready markers. Manual waits on [mutex].
     */
    suspend fun verifyForReclaim(
        userId: UserId,
        candidates: List<SyncState>,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ) = mutex.withLock {
        verifyForReclaimLocked(userId, candidates, onProgress)
    }

    /**
     * Scheduled-path entry: [tryLock] and skip when manual verify holds the mutex. Returns false when
     * skipped so the worker can end success without dumping into the delete-after-backup URI queue.
     */
    suspend fun tryVerifyForReclaim(
        userId: UserId,
        candidates: List<SyncState>,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (!mutex.tryLock()) return false
        try {
            verifyForReclaimLocked(userId, candidates, onProgress)
            return true
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun verifyForReclaimLocked(
        userId: UserId,
        candidates: List<SyncState>,
        onProgress: (done: Int, total: Int) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val checkable = candidates.filter { !it.cloudFileId.isNullOrBlank() }
        val total = checkable.size
        if (total == 0) {
            onProgress(0, 0)
            return@withContext
        }
        val sinceMs = System.currentTimeMillis() - VERIFY_FRESHNESS_MS
        val alreadyFresh = freeUpVerifiedDao.freshLocalUris(userId.id, sinceMs).toHashSet()
        val pending = checkable.filter { it.localUri !in alreadyFresh }
        var done = checkable.size - pending.size
        onProgress(done, total)
        if (pending.isEmpty()) return@withContext

        val volumeId = cloudRepo.getVolumeId(userId)
        for (chunk in pending.chunked(VERIFY_CHUNK_SIZE)) {
            val linkIds = chunk.mapNotNull { it.cloudFileId }.distinct()
            val details = retryWithBackoff(
                shouldRetry = { isTransientApiError(it) || isOfflineError(it) },
            ) {
                linkDetailHelpers.batchFetchLinkDetailsStrict(userId, volumeId, linkIds)
            }
            val now = System.currentTimeMillis()
            val verifiedRows = chunk.mapNotNull { row ->
                val linkId = row.cloudFileId ?: return@mapNotNull null
                if (details[linkId]?.link?.state != ACTIVE_LINK_STATE) return@mapNotNull null
                FreeUpVerifiedEntity(
                    userId = userId.id,
                    localUri = row.localUri,
                    cloudFileId = linkId,
                    verifiedAtMs = now,
                )
            }
            if (verifiedRows.isNotEmpty()) {
                freeUpVerifiedDao.upsert(verifiedRows)
            }
            done += chunk.size
            onProgress(done, total)
        }
    }

    /**
     * SYNCED rows whose `free_up_verified` marker still matches [cloudFileId] and is fresh.
     */
    suspend fun readyToReclaim(userId: UserId): List<SyncState> = withContext(Dispatchers.IO) {
        val sinceMs = System.currentTimeMillis() - VERIFY_FRESHNESS_MS
        freeUpVerifiedDao.readyToReclaim(userId.id, sinceMs).map { it.toDomain() }
    }

    /** Clears verified markers for [userId] (manual cancel of a verify run). */
    suspend fun clearVerified(userId: UserId) = withContext(Dispatchers.IO) {
        freeUpVerifiedDao.deleteAllForUser(userId.id)
    }

    /** Clears verified markers for the given URIs only (consent deny). */
    suspend fun clearVerified(userId: UserId, uris: List<String>) = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext
        for (chunk in uris.chunked(500)) {
            freeUpVerifiedDao.deleteForUris(userId.id, chunk)
        }
    }

    /**
     * Live Drive re-check of the persisted ready set before any delete. Never reclaims on
     * [verifiedAtMs] alone — a park without this step recreates the trash-after-verify hole.
     * Drops verified markers for links that are no longer active.
     */
    suspend fun confirmReadyForReclaim(userId: UserId): List<SyncState> = withContext(Dispatchers.IO) {
        val ready = readyToReclaim(userId)
        if (ready.isEmpty()) return@withContext emptyList()
        val volumeId = cloudRepo.getVolumeId(userId)
        val linkIds = ready.mapNotNull { it.cloudFileId }.distinct()
        val details = retryWithBackoff(
            shouldRetry = { isTransientApiError(it) || isOfflineError(it) },
        ) {
            linkDetailHelpers.batchFetchLinkDetailsStrict(userId, volumeId, linkIds)
        }
        val stillActive = ready.filter { row ->
            val linkId = row.cloudFileId ?: return@filter false
            details[linkId]?.link?.state == ACTIVE_LINK_STATE
        }
        val dropped = ready.map { it.localUri }.toSet() - stillActive.map { it.localUri }.toSet()
        if (dropped.isNotEmpty()) clearVerified(userId, dropped.toList())
        stillActive
    }

    /**
     * Non-blocking attempt to take the verify/reclaim mutex. Scheduled path (PR3) skips when busy.
     */
    fun tryLock(): Boolean = mutex.tryLock()

    fun unlock() {
        mutex.unlock()
    }

    /**
     * [protectDownloaded] keeps copies the user put on the device on purpose (a download, or a delete
     * they undid) out of the sweep. The automatic schedule passes true into [candidates] before
     * [tryVerifyForReclaim]; the manual "free up space" button leaves it false, so a deliberate tap
     * still reclaims every backed-up copy as before.
     *
     * [onProgress] is called with (done, total) as the sweep advances, so a caller can show movement
     * over a run that takes minutes on a large library. It is called from the IO context this runs on.
     *
     * Selects the candidates and reclaims them in one call. Prefer [verifyForReclaim] /
     * [tryVerifyForReclaim] + the foreground permanent-delete finisher — do not use this from the
     * scheduled worker (it would bypass live Drive verify and tempt a `PENDING_DELETE_URIS` dump).
     */
    suspend operator fun invoke(
        userId: UserId,
        olderThanMs: Long,
        protectDownloaded: Boolean = false,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): FreeUpResult =
        reclaimCandidates(candidates(userId, olderThanMs, protectDownloaded), onProgress)

    /**
     * Deletes the device copy of each row in [candidates], in order, committing each on its own, and
     * reports how many were freed (or the batched consent request for files this app does not own).
     *
     * The body runs on [Dispatchers.IO] rather than the caller's context. `ContentResolver.delete` is
     * a blocking binder call and there is one per photo, so on a library of thousands the loop owns
     * whatever thread it is given for minutes.
     */
    suspend fun reclaimCandidates(
        candidates: List<SyncState>,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): FreeUpResult = withContext(Dispatchers.IO) {
        var freed = 0
        val needsDialog = mutableListOf<Pair<String, Uri>>()  // localUri → contentUri
        val total = candidates.size
        onProgress(0, total)

        for ((index, state) in candidates.withIndex()) {
            val contentUri = Uri.parse(state.localUri)
            try {
                val deleted = context.contentResolver.delete(contentUri, null, null)
                if (deleted > 0) {
                    syncStateRepo.updateStatusAndDeleteLocal(state.localUri, SyncStatus.CLOUD_ONLY)
                    freed++
                } else {
                    // delete() returned 0, likely needs permission on API 30+
                    needsDialog += state.localUri to contentUri
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // SecurityException / RecoverableSecurityException on Android 11+
                needsDialog += state.localUri to contentUri
            }
            // Each photo is committed on its own, so a run the user walks away from keeps everything
            // it already reclaimed; the next open lists what is left and picks up from there.
            val done = index + 1
            if (done % PROGRESS_STEP == 0 || done == total) onProgress(done, total)
        }

        if (needsDialog.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // One stale URI makes createDeleteRequest throw for the whole batch — prune first via a
            // chunked MediaStore _ID probe, then slice at the platform 2000-URI ceiling.
            val candidateUris = needsDialog.map { it.first }
            val existingIds = queryExistingMediaStoreRows(context.contentResolver, candidateUris).keys
            val aliveUris = urisPresentInMediaStore(candidateUris, existingIds)
            if (aliveUris.isEmpty()) return@withContext FreeUpResult.Done(freed)
            val uriByString = needsDialog.associate { it.first to it.second }
            val firstChunk = deleteRequestChunks(aliveUris).first()
            val chunkContentUris = firstChunk.mapNotNull { uriByString[it] }
            val built = runCatching {
                MediaStore.createDeleteRequest(context.contentResolver, chunkContentUris)
            }
            val pi = built.getOrNull()
            if (pi != null) {
                return@withContext FreeUpResult.NeedsPermission(pi, firstChunk)
            }
            val cause = built.exceptionOrNull()?.message ?: "createDeleteRequest failed"
            return@withContext FreeUpResult.Failed(cause, freed)
        }

        FreeUpResult.Done(freed)
    }

    companion object {
        /** How many photos pass between progress reports. One report per photo would repaint the
         *  screen thousands of times for a number the user reads as it moves. */
        private const val PROGRESS_STEP = 20

        /** Link.State the batch link-details endpoint returns for a link that exists and is not
         *  trashed. Any other value (or an absent link) means the cloud copy is not confirmed. */
        const val ACTIVE_LINK_STATE = 1

        /** Drive batch link-details chunk size (matches [LinkDetailHelpers]). */
        const val VERIFY_CHUNK_SIZE = 50

        /** How long a persisted verify marker stays valid for [readyToReclaim] / resume skip. */
        const val VERIFY_FRESHNESS_MS = 24L * 60L * 60L * 1000L

        /**
         * Whether free-up-space may reclaim the device copy behind [state]: true only for a photo
         * whose cloud copy is confirmed, i.e. a SYNCED row carrying a real backedUpAtMs stamp that
         * predates [olderThanMs]. Pure and side-effect-free so the reclamation gate can be pinned by
         * a plain JVM test.
         *
         * A LOCAL_ONLY / CLOUD_ONLY / UPLOADING / HIDDEN row, or a SYNCED row with a null backedUpAtMs
         * (only name/size-paired to a cloud photo, never actually uploaded), is never eligible:
         * deleting its device file could destroy the only copy of an un-backed-up original.
         *
         * [protectDownloaded] adds one more exclusion for the AUTOMATIC schedule only: a copy the user
         * put on the device on purpose. An upload computes a real content hash for the row; a photo
         * that instead came FROM the cloud onto the device carries an empty hash (a download, or a
         * delete the user just undid, which re-links it SYNCED). The background sweep leaves those be,
         * since silently removing a copy the user placed would fight the intent that placed it. The
         * manual button passes false, so a deliberate "free up space" tap still reclaims them as before.
         */
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
