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

/** The outcome of probing one queued delete-after-backup URI. */
enum class PendingDeleteProbe { ALIVE, STALE, UNKNOWN }

/**
 * Whether a queued delete-after-backup URI is done with and can leave the queue. Pure and
 * side-effect-free so it can be pinned by a plain JVM test.
 */
fun pendingDeleteEntryIsStale(rowExists: Boolean, isTrashed: Boolean): Boolean =
    !rowExists || isTrashed

/**
 * What one probe of a queued URI proves. Only an answered query ([queried]) rules.
 */
fun pendingDeleteProbe(queried: Boolean, rowExists: Boolean, isTrashed: Boolean): PendingDeleteProbe = when {
    !queried -> PendingDeleteProbe.UNKNOWN
    pendingDeleteEntryIsStale(rowExists, isTrashed) -> PendingDeleteProbe.STALE
    else -> PendingDeleteProbe.ALIVE
}

/**
 * [alive] trimmed to at most [max] entries, dropping the longest-waiting ones. Pure.
 */
fun trimPendingDeleteQueue(alive: List<String>, max: Int): List<String> =
    if (alive.size <= max) alive
    else alive.sortedByDescending { mediaStoreRowId(it) }.take(max)

/** The trailing MediaStore row id of a queued URI, or -1 when it carries none. */
fun mediaStoreRowId(uri: String): Long =
    uri.substringAfterLast('/').toLongOrNull() ?: -1L
