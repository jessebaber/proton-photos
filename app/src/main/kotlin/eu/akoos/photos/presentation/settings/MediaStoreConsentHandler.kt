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

package eu.akoos.photos.presentation.settings

import android.app.Activity
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.akoos.photos.domain.usecase.DELETE_REQUEST_MAX_URIS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * How a [MediaStoreConsentHandler] builds the system consent request. Trash and permanent delete
 * are hard-separated sinks: free-up must use [PermanentDelete]; delete-after-backup uses [Trash].
 */
enum class MediaStoreConsentMode {
    Trash,
    PermanentDelete,
}

/**
 * Next URI batch for a MediaStore consent dialog, capped at [DELETE_REQUEST_MAX_URIS].
 * Pure helper so chunk advance can be pinned without Compose.
 */
fun nextMediaStoreConsentBatch(
    pending: Set<String>,
    max: Int = DELETE_REQUEST_MAX_URIS,
): Set<String> {
    if (pending.isEmpty() || max <= 0) return emptySet()
    return pending.toList().take(max).toSet()
}

/**
 * Whether [MediaStoreConsentHandler] should open a new system consent dialog.
 *
 * - [MediaStoreConsentMode.Trash]: growth-based re-prompt — only when [pendingSize] exceeds
 *   [lastPromptedSize]. Deny does **not** permanently stop trash; a later size increase re-prompts.
 * - [MediaStoreConsentMode.PermanentDelete]: prompt while the queue is non-empty unless the user
 *   already denied ([denyStopped]) or a dialog is [inFlight].
 */
fun shouldPromptMediaStoreConsent(
    mode: MediaStoreConsentMode,
    pendingSize: Int,
    lastPromptedSize: Int,
    inFlight: Boolean,
    denyStopped: Boolean,
): Boolean {
    if (inFlight) return false
    if (pendingSize <= 0) return false
    return when (mode) {
        MediaStoreConsentMode.Trash -> pendingSize > lastPromptedSize
        MediaStoreConsentMode.PermanentDelete -> !denyStopped
    }
}

/**
 * How [MediaStoreConsentHandler] reacts when `createTrashRequest` / `createDeleteRequest`
 * or `launcher.launch` fails (distinct from the user denying the system dialog).
 *
 * Launch failure stops PermanentDelete prompting after one retry without settling the queue —
 * it must **not** call [MediaStoreConsentHandler]'s `onDenied`.
 */
enum class MediaStoreConsentLaunchFailureAction {
    /** PermanentDelete first failure: clear `lastAttemptedBatch` so one re-entry is allowed. */
    RetryClearAttemptedBatch,
    /** PermanentDelete exhausted retries: stop prompting; leave the queue unsettled. */
    StopPrompting,
    /** Trash (or no further action beyond clearing `inFlight`). */
    None,
}

/**
 * @param failuresSoFar count of PermanentDelete launch failures for the current batch
 *   **after** recording this failure (1 on first failure).
 * @param maxRetries how many failures may clear the attempted batch before stopping (default 1).
 */
fun mediaStoreConsentLaunchFailureAction(
    mode: MediaStoreConsentMode,
    failuresSoFar: Int,
    maxRetries: Int = 1,
): MediaStoreConsentLaunchFailureAction {
    if (mode != MediaStoreConsentMode.PermanentDelete) {
        return MediaStoreConsentLaunchFailureAction.None
    }
    return if (failuresSoFar <= maxRetries) {
        MediaStoreConsentLaunchFailureAction.RetryClearAttemptedBatch
    } else {
        MediaStoreConsentLaunchFailureAction.StopPrompting
    }
}

/**
 * Parameterised MediaStore consent prompt: watches [pendingUris], builds a request with [mode],
 * and reports grant/deny. Callers supply their own URI source and callbacks so trash
 * (`PENDING_DELETE_URIS`) and free-up permanent delete never share a sink.
 *
 * Trash restores upstream growth re-prompt via a [rememberSaveable] int high-water mark only —
 * never saveable URI sets (TransactionTooLarge risk). Permanent delete drains
 * [DELETE_REQUEST_MAX_URIS] chunks; deny stops further chunks until the sink is emptied.
 */
@Composable
fun MediaStoreConsentHandler(
    pendingUris: StateFlow<Set<String>>,
    mode: MediaStoreConsentMode,
    settleDelayMs: Long = 3_000L,
    onGranted: (Set<String>) -> Unit,
    onDenied: (Set<String>) -> Unit = {},
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pending by pendingUris.collectAsStateWithLifecycle()
    var lastBatch by remember { mutableStateOf<Set<String>>(emptySet()) }
    var inFlight by remember { mutableStateOf(false) }
    // Not saveable: URI sets must not ride in the Bundle across rotation.
    var lastAttemptedBatch by remember { mutableStateOf<Set<String>>(emptySet()) }
    // PermanentDelete only: deny stops chunk advance until the sink drains.
    var denyStopped by remember { mutableStateOf(false) }
    // PermanentDelete only: launch failures for the current attempted batch (one retry).
    var launchFailCount by remember { mutableIntStateOf(0) }
    // Trash only: int high-water for growth re-prompt; survives config change.
    var lastPromptedSize by rememberSaveable { mutableIntStateOf(0) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        inFlight = false
        if (result.resultCode == Activity.RESULT_OK) {
            launchFailCount = 0
            if (mode == MediaStoreConsentMode.Trash) {
                // Batch settled — allow remaining / next growth to prompt again.
                lastPromptedSize = 0
            }
            onGranted(lastBatch)
        } else {
            if (mode == MediaStoreConsentMode.PermanentDelete) {
                denyStopped = true
            }
            // Trash: leave lastPromptedSize so only a later size increase re-prompts.
            onDenied(lastBatch)
        }
    }

    LaunchedEffect(pending.size, inFlight, denyStopped, lastPromptedSize, mode) {
        if (pending.isEmpty()) {
            lastAttemptedBatch = emptySet()
            denyStopped = false
            launchFailCount = 0
            lastPromptedSize = 0
            return@LaunchedEffect
        }
        if (!shouldPromptMediaStoreConsent(
                mode = mode,
                pendingSize = pending.size,
                lastPromptedSize = lastPromptedSize,
                inFlight = inFlight,
                denyStopped = denyStopped,
            )
        ) {
            return@LaunchedEffect
        }
        val batch = nextMediaStoreConsentBatch(pending)
        if (batch.isEmpty()) return@LaunchedEffect
        if (mode == MediaStoreConsentMode.PermanentDelete && batch == lastAttemptedBatch) {
            return@LaunchedEffect
        }
        delay(settleDelayMs)
        val snapshot = pendingUris.value
        if (snapshot.isEmpty()) return@LaunchedEffect
        if (!shouldPromptMediaStoreConsent(
                mode = mode,
                pendingSize = snapshot.size,
                lastPromptedSize = lastPromptedSize,
                inFlight = false,
                denyStopped = denyStopped,
            )
        ) {
            return@LaunchedEffect
        }
        val currentBatch = nextMediaStoreConsentBatch(snapshot)
        if (currentBatch.isEmpty()) return@LaunchedEffect
        if (mode == MediaStoreConsentMode.PermanentDelete && currentBatch == lastAttemptedBatch) {
            return@LaunchedEffect
        }
        val uris: List<Uri> = currentBatch.mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
        if (uris.isEmpty()) return@LaunchedEffect
        inFlight = true
        lastBatch = currentBatch
        // New attempted batch — reset per-batch launch failure budget.
        if (currentBatch != lastAttemptedBatch) {
            launchFailCount = 0
        }
        lastAttemptedBatch = currentBatch
        if (mode == MediaStoreConsentMode.Trash) {
            lastPromptedSize = snapshot.size
        }
        scope.launch {
            runCatching {
                val resolver = context.contentResolver
                val pi = withContext(Dispatchers.IO) {
                    when (mode) {
                        MediaStoreConsentMode.Trash ->
                            android.provider.MediaStore.createTrashRequest(resolver, uris, true)
                        MediaStoreConsentMode.PermanentDelete ->
                            android.provider.MediaStore.createDeleteRequest(resolver, uris)
                    }
                }
                launcher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
            }.onFailure {
                inFlight = false
                when (
                    mediaStoreConsentLaunchFailureAction(
                        mode = mode,
                        failuresSoFar = launchFailCount + 1,
                    )
                ) {
                    MediaStoreConsentLaunchFailureAction.RetryClearAttemptedBatch -> {
                        launchFailCount += 1
                        lastAttemptedBatch = emptySet()
                    }
                    MediaStoreConsentLaunchFailureAction.StopPrompting -> {
                        launchFailCount += 1
                        denyStopped = true
                        // Do not clear lastAttemptedBatch; do not call onDenied.
                    }
                    MediaStoreConsentLaunchFailureAction.None -> Unit
                }
            }
        }
    }
}
