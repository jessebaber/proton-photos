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

import eu.akoos.photos.domain.usecase.DeletePhotoResult

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.domain.entity.UserId
import eu.akoos.photos.domain.entity.GalleryItem
import eu.akoos.photos.domain.entity.SyncStatus
import eu.akoos.photos.domain.usecase.DEVICE_REQUEST_CHUNK
import eu.akoos.photos.domain.usecase.DeletePhotoUseCase
import eu.akoos.photos.domain.repository.DrivePhotoRepository
import eu.akoos.photos.domain.repository.LocalMediaRepository
import eu.akoos.photos.domain.repository.SyncStateRepository
import javax.inject.Inject

private const val TAG = "DeletePhotoUseCase"


/** The device side of a delete, apart from the use case so a test can stand in for MediaStore. */
interface DeviceMediaDeletes {
    /** Whether device files go through a system request the user confirms, which Android 11 brings,
     *  rather than [delete]. */
    val asksConsent: Boolean

    /** The Android 11+ system request that trashes [uris], or removes them for good when [permanent]. */
    fun request(uris: List<Uri>, permanent: Boolean): PendingIntent

    /** Removes one device file directly, the only way before Android 11. True once the file is gone. */
    fun delete(uri: Uri): Boolean
}

class DeletePhotoUseCaseImpl internal constructor(
    private val syncStateRepo: SyncStateRepository,
    private val cloudRepo: DrivePhotoRepository,
    private val device: DeviceMediaDeletes,
    private val localRepo: LocalMediaRepository? = null,
) : DeletePhotoUseCase<PendingIntent> {
    @Inject constructor(
        @ApplicationContext context: Context,
        syncStateRepo: SyncStateRepository,
        cloudRepo: DrivePhotoRepository,
        localRepo: LocalMediaRepository,
    ) : this(syncStateRepo, cloudRepo, MediaStoreDeletes(context), localRepo)

    /**
     * @param hide When true, this is a HIDE operation, not a regular delete:
     *   - The local URI is permanently removed via [MediaStore.createDeleteRequest] (no system
     *     trash, since the file is already preserved in the app-private Hidden vault).
     *   - The post-confirm SyncState is set to [SyncStatus.HIDDEN] instead of [SyncStatus.CLOUD_ONLY]
     *     so reconcile / cleanup skip it and the cloud copy can be rendered with a hidden-eye
     *     overlay rather than appearing as a generic cloud-only photo.
     */
    override suspend operator fun invoke(
        userId: UserId?,
        items: List<GalleryItem>,
        freeUpSpace: Boolean,
        deleteFromCloud: Boolean,
        hide: Boolean,
    ): DeletePhotoResult<PendingIntent> {
        val (cloudLinkIds, localUriStrings) = DeletePhotoUseCase.computeDeleteTargets(items, freeUpSpace, deleteFromCloud)

        // Batch shape up front: item count (overload signal) plus the type mix and cloud/local
        // split, so a delete that later fails is diagnosable from logcat rather than opaque.
        Log.d(
            TAG,
            "delete: items=${items.size} " +
                "[synced=${items.count { it is GalleryItem.Synced }}, " +
                "cloudOnly=${items.count { it is GalleryItem.CloudOnly }}, " +
                "localOnly=${items.count { it is GalleryItem.LocalOnly }}], " +
                "cloudLinks=${cloudLinkIds.size}, localUris=${localUriStrings.size}, " +
                "freeUp=$freeUpSpace, fromCloud=$deleteFromCloud, hide=$hide"
        )

        // Android 11+ with local URIs: defer the cloud delete until the user confirms the
        // system trash dialog. Returning here means cancel ↔ no cloud delete, no divergence.
        // HIDE flows use createDeleteRequest (permanent removal) instead of createTrashRequest:
        // the file is already preserved in the app-private Hidden vault, so leaving a copy in
        // MediaStore Trash for 30 days would just clutter the user's Recently Deleted list.
        if (device.asksConsent && localUriStrings.isNotEmpty()) {
            val chunks = DeletePhotoUseCase.requestChunks(items, freeUpSpace)
            return request(
                chunk = chunks.first(),
                remaining = chunks.drop(1).flatten(),
                done = emptyList(),
                freeUpSpace = freeUpSpace,
                deleteFromCloud = deleteFromCloud,
                hide = hide,
            )
        }

        if (localUriStrings.isEmpty()) {
            if (cloudLinkIds.isNotEmpty() && !trashInCloud(userId, cloudLinkIds)) return DeletePhotoResult.CloudDeleteFailed
            return DeletePhotoResult.Success()
        }

        // Before Android 11 each device file is removed directly and checked on its own: Android 10
        // refuses another app's file, and a refused photo keeps its row and its Drive copy exactly
        // as they were. The Drive half runs after the device half for that reason.
        val gone = localUriStrings.filterTo(HashSet()) { device.delete(Uri.parse(it)) }
        val kept = items.filter { item -> DeletePhotoUseCase.deviceUriOf(item, freeUpSpace)?.let { it !in gone } == true }
        if (kept.isNotEmpty()) {
            Log.w(TAG, "Direct delete left ${kept.size}/${localUriStrings.size} device file(s) in place")
        }
        val outcome = DeletePhotoResult.Success(kept)
        val settled = outcome.landed(items)
        localRepo?.forgetRemoved(gone)
        updateSyncStateAfterLocalDelete(settled, freeUpSpace, hide)
        val settledLinks =
            if (kept.isEmpty()) cloudLinkIds
            else DeletePhotoUseCase.computeDeleteTargets(settled, freeUpSpace, deleteFromCloud).cloudLinkIds
        if (settledLinks.isNotEmpty() && !trashInCloud(userId, settledLinks)) return DeletePhotoResult.CloudDeleteFailed
        return outcome
    }

    /**
     * Called by the caller AFTER the user confirmed the Android 11+ system trash dialog. Runs the
     * deferred cloud delete and updates SyncState for the items whose local copy was trashed.
     */
    override suspend fun completeAfterPermissionGranted(
        userId: UserId?,
        cloudLinkIds: List<String>,
        items: List<GalleryItem>,
        freeUpSpace: Boolean,
        hide: Boolean,
    ): DeletePhotoResult<PendingIntent> {
        // The system dialog was confirmed, so the device files are gone whatever the Drive half does next.
        localRepo?.forgetRemoved(DeletePhotoUseCase.computeDeleteTargets(items, freeUpSpace, deleteFromCloud = false).localUriStrings)
        if (cloudLinkIds.isNotEmpty() && !trashInCloud(userId, cloudLinkIds)) return DeletePhotoResult.CloudDeleteFailed
        updateSyncStateAfterLocalDelete(items, freeUpSpace, hide)
        return DeletePhotoResult.Success()
    }

    /**
     * Finishes the request [pending] names once its dialog was confirmed, and asks for the next chunk
     * of the same delete when one is left: a [DeletePhotoResult.NeedsMediaWritePermission<PendingIntent>] the caller launches
     * like the first. A chunk whose Drive half fails ends the delete there. A next request that
     * cannot be built ends it too, reporting what it leaves in [DeletePhotoResult.Success.keptOnDevice].
     */
    override suspend fun completeAfterPermissionGranted(
        userId: UserId?,
        pending: DeletePhotoResult.NeedsMediaWritePermission<PendingIntent>,
    ): DeletePhotoResult<PendingIntent> {
        val landed = completeAfterPermissionGranted(
            userId, pending.cloudLinkIds, pending.itemsBeingDeleted, pending.freeUpSpace, pending.hide,
        )
        if (landed !is DeletePhotoResult.Success || pending.remaining.isEmpty()) return landed
        val next = pending.remaining.take(DEVICE_REQUEST_CHUNK)
        val following = request(
            chunk = next,
            remaining = pending.remaining.drop(next.size),
            done = pending.confirmedItems,
            freeUpSpace = pending.freeUpSpace,
            deleteFromCloud = pending.deleteFromCloud,
            hide = pending.hide,
        )
        return if (following is DeletePhotoResult.CloudDeleteFailed) DeletePhotoResult.Success(keptOnDevice = pending.remaining) else following
    }

    /** The system request for [chunk], or [DeletePhotoResult.CloudDeleteFailed] when it cannot be built. */
    private fun request(
        chunk: List<GalleryItem>,
        remaining: List<GalleryItem>,
        done: List<GalleryItem>,
        freeUpSpace: Boolean,
        deleteFromCloud: Boolean,
        hide: Boolean,
    ): DeletePhotoResult<PendingIntent> {
        val targets = DeletePhotoUseCase.computeDeleteTargets(chunk, freeUpSpace, deleteFromCloud)
        val pi = try {
            device.request(targets.localUriStrings.map { Uri.parse(it) }, permanent = hide)
        } catch (e: Exception) {
            // A foreign or unindexable URI can throw while building the request. Fail the delete
            // safely instead of crashing the caller; nothing of this chunk was touched.
            Log.e(TAG, "Building the trash/delete request failed for ${targets.localUriStrings.size} uri(s)", e)
            return DeletePhotoResult.CloudDeleteFailed
        }
        return DeletePhotoResult.NeedsMediaWritePermission<PendingIntent>(
            pendingIntent = pi,
            cloudLinkIds = targets.cloudLinkIds,
            itemsBeingDeleted = chunk,
            freeUpSpace = freeUpSpace,
            hide = hide,
            deleteFromCloud = deleteFromCloud,
            done = done,
            remaining = remaining,
        )
    }

    /** Moves [linkIds] to Drive trash; false when none went. A cloud trash needs a signed-in user:
     *  local-only mode never yields cloud targets, so the null check only guards a caller that passes
     *  cloud links with no session, never a real guest flow. */
    private suspend fun trashInCloud(userId: UserId?, linkIds: List<String>): Boolean {
        if (userId == null) {
            Log.w(TAG, "Cloud delete requested with no signed-in user; skipping ${linkIds.size} link(s)")
            return false
        }
        return try {
            val outcome = cloudRepo.deleteFiles(userId, linkIds)
            // The bulk endpoint can accept the batch yet reject individual links (per-link
            // code != 1000); when NONE were trashed the delete wholly failed, so surface it
            // the same way a thrown error would instead of silently claiming success.
            if (outcome.trashedLinkIds.isEmpty()) {
                Log.w(TAG, "Cloud delete: server trashed 0/${linkIds.size} link(s)")
                false
            } else {
                if (outcome.failedLinkIds.isNotEmpty()) {
                    Log.w(TAG, "Cloud delete: ${outcome.failedLinkIds.size}/${linkIds.size} link(s) stayed on cloud")
                }
                true
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "Cloud delete failed for ${linkIds.size} link(s)", e)
            false
        }
    }

    /**
     * Reconciles each row's SyncStatus after the local MediaStore copy was already removed by the
     * caller above. Despite its name, [SyncStateRepository.updateStatusAndDeleteLocal] only UPDATES
     * the status column; it deletes nothing itself.
     */
    private suspend fun updateSyncStateAfterLocalDelete(items: List<GalleryItem>, freeUpSpace: Boolean, hide: Boolean) {
        for (item in items) {
            try {
                val newStatus = DeletePhotoUseCase.postDeleteSyncStatus(item, freeUpSpace, hide)
                val localUri = when (item) {
                    is GalleryItem.LocalOnly -> item.local.uri
                    is GalleryItem.Synced -> item.local.uri
                    is GalleryItem.CloudOnly -> null
                }
                if (newStatus != null && localUri != null) {
                    syncStateRepo.updateStatusAndDeleteLocal(localUri, newStatus)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                // One item's DB update failing must not strand the rest of the batch mid-loop.
                Log.w(TAG, "Sync-state update failed for one item after local delete", e)
            }
        }
    }

}

/** [DeviceMediaDeletes] over MediaStore, the one the app runs with. */
private class MediaStoreDeletes(private val context: Context) : DeviceMediaDeletes {

    override val asksConsent: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    override fun request(uris: List<Uri>, permanent: Boolean): PendingIntent {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            throw UnsupportedOperationException("system delete requests start with Android 11")
        }
        val resolver = context.contentResolver
        return try {
            if (permanent) MediaStore.createDeleteRequest(resolver, uris)
            else MediaStore.createTrashRequest(resolver, uris, true)
        } catch (e: Exception) {
            // A MIME sample makes a size- or type-specific refusal visible in logcat.
            val types = uris.take(8).joinToString { runCatching { resolver.getType(it) }.getOrNull() ?: "?" }
            Log.e(TAG, "Request refused for ${uris.size} uri(s); sample types=[$types]")
            throw e
        }
    }

    override fun delete(uri: Uri): Boolean {
        val resolver = context.contentResolver
        val removed = try {
            resolver.delete(uri, null, null) > 0
        } catch (e: Exception) {
            // Android 10 answers another app's file with a RecoverableSecurityException.
            Log.w(TAG, "Direct delete refused for one uri: ${e.javaClass.simpleName}")
            return false
        }
        if (removed) return true
        // Nothing removed: a file already gone counts as deleted, one that cannot be asked about as kept.
        return try {
            resolver.query(uri, arrayOf(MediaStore.MediaColumns._ID), null, null, null)
                ?.use { !it.moveToFirst() } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
