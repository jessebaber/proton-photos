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

import eu.akoos.photos.domain.usecase.MoveToFolderResult

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import eu.akoos.photos.domain.usecase.FolderMoveAlbumFollow
import eu.akoos.photos.domain.usecase.MovedPhoto
import eu.akoos.photos.domain.usecase.MoveToFolderUseCase
import eu.akoos.photos.domain.usecase.PendingMove
import eu.akoos.photos.domain.usecase.moveTargetRelativePath
import javax.inject.Inject

private const val TAG = "MoveToFolderUseCase"

/**
 * Moves device photos and videos into a real on-device folder under DCIM/&lt;name&gt;/ by updating each
 * MediaStore row's [MediaStore.MediaColumns.RELATIVE_PATH] IN PLACE. The file physically relocates yet
 * keeps the same content URI and _id, so the Drive `sync_state` row (keyed on the URI) stays paired to
 * it. The move is never a copy-and-delete: that would mint a new URI and orphan the pairing.
 *
 * An in-place RELATIVE_PATH update is a scoped-storage operation and needs Android 10+ (API 29). A row
 * the app owns updates silently; a foreign camera-roll file raises a recoverable [SecurityException],
 * which is collected and turned into a single system write request the caller launches before replaying
 * the move on the granted URIs. Every update runs on [Dispatchers.IO].
 */
class MoveToFolderUseCaseImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val albumFollow: FolderMoveAlbumFollow,
) : MoveToFolderUseCase<IntentSender> {
    override suspend operator fun invoke(uris: List<String>, folderName: String): MoveToFolderResult<IntentSender> =
        withContext(Dispatchers.IO) {
            val relPath = runCatching { moveTargetRelativePath(folderName) }.getOrNull()
                ?: return@withContext MoveToFolderResult.Failed("folder name is blank")
            // RELATIVE_PATH is a scoped-storage column (Android 10+). Below Q there is no in-place move,
            // and this use case is only reached from a Q+-gated action, so fail defensively rather than
            // silently do nothing.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return@withContext MoveToFolderResult.Failed("in-place move needs Android 10+")
            }
            moveAll(uris, relPath, folderName)
        }

    /** Replays the deferred move after the user granted the system write request. */
    override suspend fun completeAfterPermissionGranted(pending: PendingMove): MoveToFolderResult<IntentSender> =
        withContext(Dispatchers.IO) {
            val relPath = runCatching { moveTargetRelativePath(pending.folderName) }.getOrNull()
                ?: return@withContext MoveToFolderResult.Failed("folder name is blank")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return@withContext MoveToFolderResult.Failed("in-place move needs Android 10+")
            }
            moveAll(pending.uris, relPath, pending.folderName)
        }

    private fun moveAll(uris: List<String>, relPath: String, folderName: String): MoveToFolderResult<IntentSender> {
        // Parse defensively so one malformed URI string cannot abort the whole batch.
        val parsed = uris.mapNotNull { s -> runCatching { Uri.parse(s) }.getOrNull()?.let { s to it } }
        if (parsed.isEmpty()) return MoveToFolderResult.NothingToDo

        Log.d(TAG, "move: uris=${uris.size} parsed=${parsed.size} -> $relPath")

        var moved = 0
        // The folder each moved photo left and its name, read before the move, so the Drive albums of mirrored
        // folders can follow it (see [FolderMoveAlbumFollow]).
        val movedPhotos = mutableListOf<MovedPhoto>()
        val foreign = mutableListOf<ForeignUri>()
        for ((uriString, uri) in parsed) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
                }
                val before = bucketAndNameOf(uri)
                if (context.contentResolver.update(uri, values, null, null) > 0) {
                    moved++
                    movedPhotos += MovedPhoto(uriString, before?.bucket, before?.name, before?.takenMs)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                // A file the app does not own refuses a silent scoped-storage write; defer it for the
                // one-shot system consent request built below.
                foreign += ForeignUri(uriString, uri, e)
            } catch (e: Exception) {
                Log.w(TAG, "Move failed for one uri", e)
            }
        }

        if (movedPhotos.isNotEmpty()) {
            albumFollow.onPhotosMoved(movedPhotos, relPath.removePrefix("DCIM/").removeSuffix("/"))
        }

        if (foreign.isNotEmpty()) {
            val sender = buildWriteConsent(foreign)
            if (sender != null) {
                Log.d(TAG, "move: $moved relocated silently, ${foreign.size} need write consent")
                return MoveToFolderResult.NeedsPermission(sender, PendingMove(foreign.map { it.uriString }, folderName))
            }
            // Building the request failed on a platform that should support it; report whatever moved
            // silently rather than stranding the batch with no signal.
            Log.w(TAG, "Could not build write consent for ${foreign.size} foreign uri(s); moved=$moved")
        }

        return if (moved > 0) MoveToFolderResult.Moved(moved) else MoveToFolderResult.NothingToDo
    }

    /** The device folder (MediaStore bucket name) [uri] is in now and its file name, or null when unreadable. */
    private data class Before(val bucket: String?, val name: String?, val takenMs: Long?)

    private fun bucketAndNameOf(uri: Uri): Before? = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(
                MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_TAKEN,
            ),
            null, null, null,
        )?.use { c ->
            if (c.moveToFirst()) Before(c.getString(0), c.getString(1), if (c.isNull(2)) null else c.getLong(2)) else null
        }
    }.getOrNull()

    /**
     * The system write-consent request covering the [foreign] files. Android 11+ batches them all into
     * one [MediaStore.createWriteRequest]; Android 10 has no batch API, so the first recoverable file's
     * own action is used and the caller replays the move, re-prompting for any file still refused.
     */
    private fun buildWriteConsent(foreign: List<ForeignUri>): IntentSender? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return runCatching {
                MediaStore.createWriteRequest(context.contentResolver, foreign.map { it.uri }).intentSender
            }.getOrNull()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return foreign
                .mapNotNull { (it.cause as? RecoverableSecurityException)?.userAction?.actionIntent?.intentSender }
                .firstOrNull()
        }
        return null
    }

    private data class ForeignUri(
        val uriString: String,
        val uri: Uri,
        val cause: SecurityException,
    )
}

