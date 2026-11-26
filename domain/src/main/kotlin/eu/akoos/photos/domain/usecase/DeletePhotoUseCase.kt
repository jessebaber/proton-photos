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

import eu.akoos.photos.domain.entity.GalleryItem
import eu.akoos.photos.domain.entity.SyncStatus
import me.proton.core.domain.entity.UserId

/** Device uris one system trash or delete request carries. The request crosses a Binder transaction,
 *  which a library-sized batch outgrows, so a larger delete asks once per chunk. */
const val DEVICE_REQUEST_CHUNK = 1_000

sealed interface DeletePhotoResult<out Consent> {
    /**
     * The delete went through. [keptOnDevice] holds the items whose device file is still there:
     * a direct delete was refused (Android 10 refuses another app's file without a per-file
     * consent), or a follow-up system request could not be built. Their rows and Drive copies
     * are left as they were.
     */
    data class Success(val keptOnDevice: List<GalleryItem> = emptyList()) : DeletePhotoResult<Nothing> {
        /** [items] less the ones [keptOnDevice] names: what the delete carried out. */
        fun landed(items: List<GalleryItem>): List<GalleryItem> =
            if (keptOnDevice.isEmpty()) items
            else keptOnDevice.toHashSet().let { kept -> items.filterNot { it in kept } }
    }

    /**
     * Android 11+ wants the user to confirm the system trash dialog. [cloudLinkIds] are the
     * Drive linkIds to delete AFTER the user confirms — passing them here defers the cloud
     * delete so a cancel cannot leave the cloud copy trashed while the local copy survives.
     *
     * A delete of more than [DEVICE_REQUEST_CHUNK] device files asks in chunks, one dialog each:
     * [itemsBeingDeleted] is this request's share, [done] what earlier requests of the same delete
     * already carried out, and [remaining] what still waits for a request of its own.
     */
    data class NeedsMediaWritePermission<Consent>(
        val pendingIntent: Consent,
        val cloudLinkIds: List<String>,
        val itemsBeingDeleted: List<GalleryItem>,
        val freeUpSpace: Boolean,
        /** Whether this delete is part of a HIDE flow — controls whether the post-confirm
         *  SyncState transitions to HIDDEN (hide) or CLOUD_ONLY (regular free-up-space). */
        val hide: Boolean,
        /** Whether the delete takes the Drive copies too, for the whole delete, not only this chunk. */
        val deleteFromCloud: Boolean = cloudLinkIds.isNotEmpty(),
        val done: List<GalleryItem> = emptyList(),
        val remaining: List<GalleryItem> = emptyList(),
    ) : DeletePhotoResult<Consent> {
        /** Everything the delete has carried out once this request is granted too. */
        val confirmedItems: List<GalleryItem> get() = done + itemsBeingDeleted
    }

    data object CloudDeleteFailed : DeletePhotoResult<Nothing>
}

/** Cloud linkIds to trash and device URI strings to delete, produced by [DeletePhotoUseCase.computeDeleteTargets]. */
data class DeleteTargets(
    val cloudLinkIds: List<String>,
    val localUriStrings: List<String>,
)

/**
 * Deletes gallery items from the device and/or Drive. [C] is the platform consent payload the
 * system delete dialog carries ([android.app.PendingIntent] in the Android binding).
 */
interface DeletePhotoUseCase<C> {
    /**
     * @param hide When true, this is a HIDE operation, not a regular delete:
     *   - The local URI is permanently removed (no system trash, since the file is already
     *     preserved in the app-private Hidden vault).
     *   - The post-confirm SyncState is set to [SyncStatus.HIDDEN] instead of [SyncStatus.CLOUD_ONLY]
     *     so reconcile / cleanup skip it and the cloud copy can be rendered with a hidden-eye
     *     overlay rather than appearing as a generic cloud-only photo.
     */
    suspend operator fun invoke(
        userId: UserId?,
        items: List<GalleryItem>,
        freeUpSpace: Boolean,
        deleteFromCloud: Boolean,
        hide: Boolean = false,
    ): DeletePhotoResult<C>

    /**
     * Called by the caller AFTER the user confirmed the Android 11+ system trash dialog. Runs the
     * deferred cloud delete and updates SyncState for the items whose local copy was trashed.
     */
    suspend fun completeAfterPermissionGranted(
        userId: UserId?,
        cloudLinkIds: List<String>,
        items: List<GalleryItem>,
        freeUpSpace: Boolean,
        hide: Boolean = false,
    ): DeletePhotoResult<C>

    /**
     * Finishes the request [pending] names once its dialog was confirmed, and asks for the next chunk
     * of the same delete when one is left.
     */
    suspend fun completeAfterPermissionGranted(
        userId: UserId?,
        pending: DeletePhotoResult.NeedsMediaWritePermission<C>,
    ): DeletePhotoResult<C>

    companion object {
        fun computeDeleteTargets(
            items: List<GalleryItem>,
            freeUpSpace: Boolean,
            deleteFromCloud: Boolean,
        ): DeleteTargets {
            val cloudLinkIds = items.mapNotNull { item ->
                when {
                    deleteFromCloud && item is GalleryItem.Synced -> item.cloud.linkId
                    deleteFromCloud && item is GalleryItem.CloudOnly -> item.cloud.linkId
                    else -> null
                }
            }
            val localUriStrings = items.mapNotNull { deviceUriOf(it, freeUpSpace) }
            return DeleteTargets(cloudLinkIds, localUriStrings)
        }

        fun deviceUriOf(item: GalleryItem, freeUpSpace: Boolean): String? = when {
            !freeUpSpace -> null
            item is GalleryItem.LocalOnly -> item.local.uri
            item is GalleryItem.Synced -> item.local.uri
            else -> null
        }

        fun requestChunks(
            items: List<GalleryItem>,
            freeUpSpace: Boolean,
            maxUris: Int = DEVICE_REQUEST_CHUNK,
        ): List<List<GalleryItem>> {
            val (onDevice, elsewhere) = items.partition { deviceUriOf(it, freeUpSpace) != null }
            if (onDevice.isEmpty()) return listOf(items)
            val chunks = onDevice.chunked(maxUris)
            return listOf(elsewhere + chunks.first()) + chunks.drop(1)
        }

        fun postDeleteSyncStatus(item: GalleryItem, freeUpSpace: Boolean, hide: Boolean): SyncStatus? =
            when {
                item is GalleryItem.LocalOnly -> SyncStatus.LOCAL_ONLY
                item is GalleryItem.Synced && hide -> SyncStatus.HIDDEN
                item is GalleryItem.Synced && freeUpSpace -> SyncStatus.CLOUD_ONLY
                else -> null
            }

        fun removesFromGallery(item: GalleryItem, freeUpSpace: Boolean, deleteFromCloud: Boolean): Boolean =
            when (item) {
                is GalleryItem.LocalOnly -> freeUpSpace
                is GalleryItem.CloudOnly -> deleteFromCloud
                is GalleryItem.Synced -> freeUpSpace && deleteFromCloud
            }
    }
}
