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

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

/** Match [eu.akoos.photos.util.SQL_CHUNK_SIZE]: SQLite host-variable floor is 999 on older Android. */
private const val MEDIA_STORE_ID_CHUNK = 500

/**
 * Trailing MediaStore row id of a content URI, or null when the last path segment is not numeric.
 * Pure so reclaim / queue pruning can classify URIs without touching the provider.
 */
fun mediaStoreRowId(uri: String): Long? =
    uri.substringAfterLast('/').substringBefore('?').toLongOrNull()

/**
 * Which of [uriStrings] still name a MediaStore row whose id appears in [existingIds]. Pure.
 *
 * A URI with no numeric id is treated as missing: createDeleteRequest / createTrashRequest cannot
 * act on it, and leaving it in a consent batch is what makes the whole request throw.
 */
fun urisPresentInMediaStore(uriStrings: Collection<String>, existingIds: Set<Long>): List<String> =
    uriStrings.filter { uri ->
        val id = mediaStoreRowId(uri) ?: return@filter false
        id in existingIds
    }

/**
 * Presence of a MediaStore row keyed by [_ID], with the trashed flag when the column is available
 * (API 30+). Used to prune stale URIs before building a delete/trash consent request.
 */
data class MediaStoreRowPresence(
    val id: Long,
    val isTrashed: Boolean,
)

/**
 * Bulk-probe which MediaStore row ids among [uriStrings] still exist, via chunked
 * `MediaStore.Files` `_ID IN (…)` queries (at [MEDIA_STORE_ID_CHUNK]). Prefer this over one query
 * per URI when pruning large free-up or pending-delete batches.
 *
 * Returns a map keyed by row id. An empty input yields an empty map without touching the resolver.
 */
fun queryExistingMediaStoreRows(
    resolver: ContentResolver,
    uriStrings: Collection<String>,
): Map<Long, MediaStoreRowPresence> {
    val ids = uriStrings.mapNotNull { mediaStoreRowId(it) }.distinct()
    if (ids.isEmpty()) return emptyMap()
    val collection = MediaStore.Files.getContentUri("external")
    val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.MediaColumns.IS_TRASHED)
    } else {
        arrayOf(MediaStore.Files.FileColumns._ID)
    }
    val found = LinkedHashMap<Long, MediaStoreRowPresence>(ids.size)
    for (chunk in ids.chunked(MEDIA_STORE_ID_CHUNK)) {
        val placeholders = chunk.joinToString(",") { "?" }
        val selection = "${MediaStore.Files.FileColumns._ID} IN ($placeholders)"
        val args = Array(chunk.size) { i -> chunk[i].toString() }
        resolver.query(collection, projection, selection, args, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val trashCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                cursor.getColumnIndex(MediaStore.MediaColumns.IS_TRASHED)
            } else {
                -1
            }
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val trashed = trashCol >= 0 && cursor.getInt(trashCol) != 0
                found[id] = MediaStoreRowPresence(id = id, isTrashed = trashed)
            }
        }
    }
    return found
}

/**
 * Parse [uriString] to a content [Uri], or null when it is not a well-formed content URI.
 */
fun parseContentUriOrNull(uriString: String): Uri? =
    runCatching { Uri.parse(uriString) }.getOrNull()?.takeIf { it.scheme != null }
