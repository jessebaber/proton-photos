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

import eu.akoos.photos.domain.usecase.ImportLedgerRow
import eu.akoos.photos.domain.usecase.UndoImportUseCase
import eu.akoos.photos.domain.usecase.UndoResult
import eu.akoos.photos.domain.usecase.planImportUndo
import eu.akoos.photos.domain.usecase.undoResultOf

import kotlin.jvm.JvmName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import eu.akoos.photos.data.db.dao.ImportUploadedDao
import eu.akoos.photos.data.repository.drive.LinkDetailHelpers
import eu.akoos.photos.domain.repository.DrivePhotoRepository
import eu.akoos.photos.util.ImportDiagnostics
import eu.akoos.photos.util.SyncDiagnostics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reverses one import run by moving the photos it uploaded to the Drive trash, but only the photos
 * whose bytes still match what the run sent. Each candidate's current cloud ContentHash is read straight
 * from Drive and compared to the hash the stored upload sha1 maps to; a photo the user has since
 * replaced or edited no longer matches and is kept. Every removal is a recoverable trash move, never a
 * permanent delete, and any uncertainty (a link the fetch could not read, an underivable expected hash,
 * a hash that differs) resolves to keeping the photo, so an undo can never take a file the run did not
 * upload or a newer version the user chose to keep.
 */
@Singleton
class UndoImportUseCaseImpl @Inject constructor(
    private val cloudRepo: DrivePhotoRepository,
    private val importUploadedDao: ImportUploadedDao,
    private val linkDetailHelpers: LinkDetailHelpers,
) : UndoImportUseCase {
    override suspend fun undo(userId: UserId, runId: String): UndoResult = withContext(Dispatchers.IO) {
        // Only the run's REAL uploads are undo candidates. A deduped row records a pre-existing Drive link
        // the run never created, so it is excluded here and can never be trashed by undoing this run.
        val rows = importUploadedDao.pendingRealUploadsByRun(runId)
        if (rows.isEmpty()) return@withContext UndoResult(0, 0, 0)

        val linkIds = rows.map { it.linkId }.distinct()

        // Current cloud ContentHash per candidate link, read from Drive in one batched call. A link the
        // fetch does not return (already gone, or the server will not surface it) is simply absent from
        // this map and is never trashed. A transient failure of the whole batch throws and is caught
        // here so the run keeps every photo rather than trashing against an answer it could not read.
        val currentByLink: Map<String, String?> = try {
            val volumeId = cloudRepo.getVolumeId(userId)
            linkDetailHelpers.batchFetchLinkDetails(userId, volumeId, linkIds)
                .mapValues { (_, detail) -> detail.photo?.contentHash }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SyncDiagnostics.log("import undo: link fetch failed, kept ${rows.size} unverified")
            ImportDiagnostics.recordError(e.javaClass.simpleName)
            ImportDiagnostics.recordUndo(0, 0, rows.size)
            return@withContext UndoResult(0, 0, rows.size)
        }

        // Expected hash = the cloud ContentHash the stored upload sha1 maps to, null when the root hash
        // key is not cached. A null expected never equals a real current hash, so the partition keeps
        // those rows instead of guessing.
        val expectedByLink: Map<String, String?> =
            rows.associate { it.linkId to cloudRepo.cloudContentHash(it.sha1) }

        val plan = planImportUndo(rows.map { it.linkId }, expectedByLink, currentByLink)

        var trashCallFailed = 0
        val outcome = if (plan.toTrash.isEmpty()) {
            null
        } else {
            try {
                cloudRepo.deleteFiles(userId, plan.toTrash)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A hard failure of the bulk trash leaves the rows pending for a later retry rather than
                // aborting the run. Nothing was trashed, so nothing is at risk.
                trashCallFailed = plan.toTrash.size
                null
            }
        }
        val trashed = outcome?.trashedLinkIds ?: emptySet()
        val failedTrash = outcome?.failedLinkIds ?: emptySet()

        if (trashed.isNotEmpty()) {
            try {
                // Chunk the ledger update so a very large undo never overflows the host-variable cap the
                // IN clause binds against (999 on older Android). The trash call above already chunks.
                trashed.toList().chunked(500).forEach { batch ->
                    importUploadedDao.markUndone(runId, batch)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The photos are trashed; a failed ledger write only means a later undo re-checks them,
                // finds them gone, and skips them, so it never double-acts.
            }
        }

        val ledgerRows = rows.map { ImportLedgerRow(it.linkId, it.name, it.alreadyInDrive) }
        val result = undoResultOf(ledgerRows, plan, trashed, failedTrash, trashCallFailed = trashCallFailed > 0)
        SyncDiagnostics.log(
            "import undo: trashed ${result.undone}, kept ${result.keptChanged} changed, failed ${result.failed}",
        )
        ImportDiagnostics.recordUndo(result.undone, result.keptChanged, result.failed)
        result
    }
}
