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

package eu.akoos.photos.data.usecase

import eu.akoos.photos.domain.usecase.FreeUpResult

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import eu.akoos.photos.data.repository.drive.LinkDetailHelpers
import eu.akoos.photos.domain.entity.SyncState
import eu.akoos.photos.domain.entity.SyncStatus
import eu.akoos.photos.domain.repository.DrivePhotoRepository
import eu.akoos.photos.domain.repository.SyncStateRepository
import eu.akoos.photos.domain.usecase.FreeUpSpaceUseCase
import javax.inject.Inject

private const val PROGRESS_STEP = 20
private const val ACTIVE_LINK_STATE = 1

class FreeUpSpaceUseCaseImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncStateRepo: SyncStateRepository,
    private val cloudRepo: DrivePhotoRepository,
    private val linkDetailHelpers: LinkDetailHelpers,
) : FreeUpSpaceUseCase<PendingIntent> {

    /**
     * Exactly the rows [invoke] would act on, for a caller that has to show the user what is about to
     * leave their device before it does. Sharing one query is the point: a list drawn from a second,
     * similar-looking filter could name a photo the sweep spares, or spare one the sweep takes, and
     * the screen would be a promise about a different set than the one that runs.
     *
     * Every row is `SYNCED`, so every photo in it has a Drive copy. That is what makes the backed-up
     * badge on each cell a check the user can make rather than a decoration.
     */
    override suspend fun candidates(
        userId: UserId,
        olderThanMs: Long,
        protectDownloaded: Boolean,
    ): List<SyncState> = withContext(Dispatchers.IO) {
        syncStateRepo.getSyncedBefore(userId, olderThanMs)
            .filter { FreeUpSpaceUseCase.isEligibleForReclamation(it, olderThanMs, protectDownloaded) }
    }

    /**
     * The subset of [candidates] whose Drive copy is confirmed active right now, so only those get
     * deleted. Each candidate's cloudFileId is looked up in one batched link-details call (chunked at
     * 50), and a candidate is kept only when its link comes back State == 1: present on Drive and not
     * trashed. Checking the candidates alone is bounded by the on-device count, where the listing walk
     * this replaced grew with the whole Drive volume and on a large library ran long enough that the
     * sweep never started.
     *
     * A candidate is DROPPED (kept on the device, never deleted) when its link is absent from the
     * returned map, comes back at any State other than 1, or carries no cloudFileId to check with. A
     * transient failure (429 / 5xx / network) from the lookup PROPAGATES so the caller aborts and
     * deletes nothing against an unconfirmed batch, rather than reading a rate-limited short map as an
     * all-trashed answer. So does any other failed batch apart from a server answer that the link
     * does not exist, which only drops that link.
     */
    override suspend fun verifyActiveBackups(
        userId: UserId,
        candidates: List<SyncState>,
    ): List<SyncState> = withContext(Dispatchers.IO) {
        if (candidates.none { !it.cloudFileId.isNullOrBlank() }) return@withContext emptyList<SyncState>()
        confirmActive(userId, cloudRepo.getVolumeId(userId), candidates)
    }

    private suspend fun confirmActive(userId: UserId, volumeId: String, rows: List<SyncState>): List<SyncState> {
        val checkable = rows.filter { !it.cloudFileId.isNullOrBlank() }
        if (checkable.isEmpty()) return emptyList()
        val linkIds = checkable.mapNotNull { it.cloudFileId }.distinct()
        val details = linkDetailHelpers.batchFetchLinkDetails(userId, volumeId, linkIds, strict = true)
        return checkable.filter { row ->
            val linkId = row.cloudFileId ?: return@filter false
            details[linkId]?.link?.state == ACTIVE_LINK_STATE
        }
    }

    /**
     * [protectDownloaded] keeps copies the user put on the device on purpose (a download, or a delete
     * they undid) out of the sweep. The automatic schedule passes true; the manual "free up space"
     * button leaves it false, so a deliberate tap still reclaims every backed-up copy as before.
     *
     * [onProgress] is called with (done, total) as the sweep advances, so a caller can show movement
     * over a run that takes minutes on a large library. It is called from the IO context this runs on.
     *
     * Selects the candidates and reclaims them in one call, with no cloud check. A caller that deletes
     * on a schedule uses [sweepConfirmed]; the manual button runs [verifyAndReclaim] itself.
     */
    override suspend operator fun invoke(
        userId: UserId,
        olderThanMs: Long,
        protectDownloaded: Boolean,
        onProgress: (done: Int, total: Int) -> Unit,
    ): FreeUpResult<PendingIntent> =
        reclaimCandidates(candidates(userId, olderThanMs, protectDownloaded), onProgress)

    /**
     * The scheduled sweep: [candidates], narrowed to the rows [verifyActiveBackups] confirms against
     * Drive right now, then reclaimed.
     *
     * The sync state alone is not enough to delete on. Reconcile stamps the cloud state as checked
     * whether or not a listing walk ran before it, which a Wi-Fi-only backup on mobile data skips, so
     * a photo already gone from Drive can still read SYNCED, and its device copy would be the last
     * one. A lookup that fails throws, so a batch that could not be confirmed deletes nothing.
     */
    override suspend fun sweepConfirmed(
        userId: UserId,
        olderThanMs: Long,
        protectDownloaded: Boolean,
        onProgress: (done: Int, total: Int) -> Unit,
    ): FreeUpResult<PendingIntent> = verifyAndReclaim(userId, candidates(userId, olderThanMs, protectDownloaded), onProgress)

    /**
     * Confirms [candidates] against Drive and deletes them a slice of [FreeUpSpaceUseCase.VERIFY_CHUNK] at a time, each
     * slice right after its own check, so no copy is deleted on an answer minutes old. A slice whose
     * check throws stops the run there: what earlier slices freed stays freed, nothing past it is
     * touched. Copies this app may not delete itself are gathered for the system dialog
     * ([consentFor]), checked again just before it.
     */
    override suspend fun verifyAndReclaim(
        userId: UserId,
        candidates: List<SyncState>,
        onProgress: (done: Int, total: Int) -> Unit,
    ): FreeUpResult<PendingIntent> = withContext(Dispatchers.IO) {
        val total = candidates.size
        onProgress(0, total)
        if (candidates.isEmpty()) return@withContext FreeUpResult.Done(0)
        val volumeId = cloudRepo.getVolumeId(userId)
        var freed = 0
        val needsDialog = mutableListOf<SyncState>()
        var done = 0
        for (slice in candidates.chunked(FreeUpSpaceUseCase.VERIFY_CHUNK)) {
            for (row in confirmActive(userId, volumeId, slice)) {
                if (deleteDeviceCopy(row)) freed++ else needsDialog += row
            }
            done += slice.size
            onProgress(done, total)
        }
        consentFor(userId, needsDialog, freed)
    }

    /**
     * The system delete dialog for [rows], which this app could not delete itself: checked against
     * Drive once more, since the dialog can come long after the sweep, and capped at
     * [DELETE_REQUEST_MAX_URIS] per dialog, the most one request may carry. [freed] is what the
     * caller already reclaimed, reported back when no dialog is needed. Below Android 11 there is no
     * dialog, so the rows are reported as skipped and stay on the device.
     */
    override suspend fun consentFor(userId: UserId, rows: List<SyncState>, freed: Int ): FreeUpResult<PendingIntent> =
        withContext(Dispatchers.IO) {
            if (rows.isEmpty()) return@withContext FreeUpResult.Done(freed)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                return@withContext FreeUpResult.Done(freed, skipped = rows.size)
            }
            val (batch, rest) = FreeUpSpaceUseCase.splitForConsent(verifyActiveBackups(userId, rows))
            if (batch.isEmpty()) return@withContext FreeUpResult.Done(freed)
            // Building the consent request can throw on some OEMs for a foreign batch; fall through to
            // Done rather than letting it escape uncaught.
            val pi = runCatching {
                MediaStore.createDeleteRequest(context.contentResolver, batch.map { Uri.parse(it.localUri) })
            }.getOrNull() ?: return@withContext FreeUpResult.Done(freed)
            FreeUpResult.NeedsPermission(pi, batch.map { it.localUri }, rest)
        }

    /** Deletes [state]'s device copy and marks its row CLOUD_ONLY; false when the system has to ask. */
    private suspend fun deleteDeviceCopy(state: SyncState): Boolean = try {
        if (context.contentResolver.delete(Uri.parse(state.localUri), null, null) > 0) {
            syncStateRepo.updateStatusAndDeleteLocal(state.localUri, SyncStatus.CLOUD_ONLY)
            true
        } else {
            // delete() returned 0, likely needs permission on API 30+
            false
        }
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        // SecurityException / RecoverableSecurityException on Android 11+
        false
    }

    /**
     * Deletes the device copy of each row in [candidates], in order, committing each on its own, and
     * reports how many were freed (or the batched consent request for files this app does not own).
     * The caller states the exact set and has checked it already, if at all: [invoke] passes what
     * [candidates] selected, with no cloud check. [verifyAndReclaim] is the checked path.
     *
     * The body runs on [Dispatchers.IO] rather than the caller's context. `ContentResolver.delete` is
     * a blocking binder call and there is one per photo, so on a library of thousands the loop owns
     * whatever thread it is given for minutes. Called from a ViewModel that is exactly the main
     * thread: the deletes still land, while the UI cannot repaint, which reads as a spinner that
     * never stops. The scheduled run never showed it because a CoroutineWorker is already off-main.
     */
    override suspend fun reclaimCandidates(
        candidates: List<SyncState>,
        onProgress: (done: Int, total: Int) -> Unit,
    ): FreeUpResult<PendingIntent> = withContext(Dispatchers.IO) {
        var freed = 0
        val needsDialog = mutableListOf<SyncState>()
        val total = candidates.size
        onProgress(0, total)

        for ((index, state) in candidates.withIndex()) {
            if (deleteDeviceCopy(state)) freed++ else needsDialog += state
            // Each photo is committed on its own, so a run the user walks away from keeps everything
            // it already reclaimed; the next open lists what is left and picks up from there.
            val done = index + 1
            if (done % PROGRESS_STEP == 0 || done == total) onProgress(done, total)
        }

        if (needsDialog.isEmpty()) return@withContext FreeUpResult.Done(freed)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return@withContext FreeUpResult.Done(freed, skipped = needsDialog.size)
        }
        val (batch, rest) = FreeUpSpaceUseCase.splitForConsent(needsDialog)
        val pi = runCatching {
            MediaStore.createDeleteRequest(context.contentResolver, batch.map { Uri.parse(it.localUri) })
        }.getOrNull() ?: return@withContext FreeUpResult.Done(freed)
        FreeUpResult.NeedsPermission(pi, batch.map { it.localUri }, rest)
    }

}
