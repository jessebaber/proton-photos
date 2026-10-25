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
import android.app.PendingIntent
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * How a [MediaStoreConsentHandler] builds the system consent request. Trash and permanent delete
 * are hard-separated sinks: free-up must use [PermanentDelete]; delete-after-backup uses [Trash].
 */
enum class MediaStoreConsentMode {
    Trash,
    PermanentDelete,
}

/**
 * Parameterised MediaStore consent prompt: watches [pendingUris], builds a request with [mode],
 * and reports grant/deny. Callers supply their own URI source and callbacks so trash
 * (`PENDING_DELETE_URIS`) and free-up permanent delete never share a sink.
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
    var lastPromptedSize by rememberSaveable { mutableStateOf(0) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        inFlight = false
        if (result.resultCode == Activity.RESULT_OK) {
            onGranted(lastBatch)
            lastPromptedSize = 0
        } else {
            onDenied(lastBatch)
        }
    }

    LaunchedEffect(pending.size, inFlight) {
        if (inFlight) return@LaunchedEffect
        if (pending.isEmpty()) {
            lastPromptedSize = 0
            return@LaunchedEffect
        }
        if (pending.size <= lastPromptedSize) return@LaunchedEffect
        delay(settleDelayMs)
        val snapshot = pendingUris.value
        if (snapshot.isEmpty()) return@LaunchedEffect
        val uris: List<Uri> = snapshot.mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
        if (uris.isEmpty()) return@LaunchedEffect
        inFlight = true
        lastBatch = snapshot
        lastPromptedSize = snapshot.size
        scope.launch {
            runCatching {
                val pi = buildConsentRequest(context.contentResolver, uris, mode)
                launcher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
            }.onFailure { inFlight = false }
        }
    }
}

internal fun buildConsentRequest(
    resolver: ContentResolver,
    uris: List<Uri>,
    mode: MediaStoreConsentMode,
): PendingIntent = when (mode) {
    MediaStoreConsentMode.Trash ->
        android.provider.MediaStore.createTrashRequest(resolver, uris, true)
    MediaStoreConsentMode.PermanentDelete ->
        android.provider.MediaStore.createDeleteRequest(resolver, uris)
}
