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

import android.util.Log
import eu.akoos.photos.domain.entity.compression.CompressionOutcome
import eu.akoos.photos.domain.entity.compression.CompressionSkipReason
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import java.util.Locale

internal const val UPLOAD_TAG = "UploadUseCase"

/**
 * One name-free diagnostics line per compression decision, so release builds (where `Log` is
 * stripped) still record it, e.g. `compress <ref> video SKIPPED ALREADY_EFFICIENT 24.3MB`. The ref is
 * the same non-reversible URI hash the other upload lines use.
 */
internal fun logCompression(
    localUri: String,
    kind: String,
    outcome: CompressionOutcome,
    reason: CompressionSkipReason?,
    sourceBytes: Long,
    outputBytes: Long? = null,
    codec: String? = null,
    detail: String? = null,
) {
    val mb = { b: Long -> "%.1fMB".format(Locale.US, b / 1_000_000.0) }
    val sizes = if (outputBytes != null && outputBytes > 0L) mb(sourceBytes) + "->" + mb(outputBytes) else mb(sourceBytes)
    val line = listOfNotNull(
        "compress", eu.akoos.photos.util.uploadLogRef(localUri), kind, outcome.name, reason?.name, codec, sizes, detail,
    ).joinToString(" ")
    Log.d(UPLOAD_TAG, line)
    eu.akoos.photos.util.SyncDiagnostics.log(line)
}

/**
 * Runs [block] with one held permit handed back. The re-acquire can't be cancelled, because the
 * enclosing `withPermit` releases one on the way out whatever happens.
 */
suspend fun <T> Semaphore.releasedWhile(block: suspend () -> T): T {
    release()
    try {
        return block()
    } finally {
        withContext(NonCancellable) { acquire() }
    }
}
