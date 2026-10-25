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

/**
 * Platform ceiling for URIs handed to [android.provider.MediaStore.createDeleteRequest] /
 * [android.provider.MediaStore.createTrashRequest] in one shot (Baklava / API 36 docs; also a
 * practical Binder size guard on earlier targets). Reclaim paths must never build a single request
 * larger than this.
 */
const val DELETE_REQUEST_MAX_URIS = 2_000

/**
 * Slice [uris] into batches of at most [max] for MediaStore delete/trash request builders.
 *
 * Pure and side-effect-free so the Binder / platform ceiling can be pinned by a plain JVM test.
 *
 * Phase 1 stub: returns the input as one chunk (no slicing) so acceptance tests stay red until
 * Phase 3 lands the real [chunked] implementation.
 */
fun deleteRequestChunks(
    uris: List<String>,
    max: Int = DELETE_REQUEST_MAX_URIS,
): List<List<String>> {
    if (uris.isEmpty()) return emptyList()
    return listOf(uris)
}
