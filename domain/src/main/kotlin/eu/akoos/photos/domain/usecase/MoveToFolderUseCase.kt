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
 * The part of a move the OS refused to run silently. A file the app does not own needs one-shot user
 * consent, so its move is deferred: the caller launches the write request from
 * [MoveToFolderResult.NeedsPermission] and, once granted, replays exactly these [uris] into
 * [folderName] through [MoveToFolderUseCase.completeAfterPermissionGranted].
 */
data class PendingMove(val uris: List<String>, val folderName: String)

sealed interface MoveToFolderResult<out Consent> {
    /** [count] rows were relocated in place. */
    data class Moved(val count: Int) : MoveToFolderResult<Nothing>

    /** One or more foreign files need one-shot write consent. The caller launches [intentSender]
     *  and, on approval, calls [MoveToFolderUseCase.completeAfterPermissionGranted] with [pending]. */
    data class NeedsPermission<Consent>(val intentSender: Consent, val pending: PendingMove) : MoveToFolderResult<Consent>

    data class Failed(val reason: String) : MoveToFolderResult<Nothing>

    /** Nothing was eligible to move (no parseable URIs, or every update was declined silently). */
    data object NothingToDo : MoveToFolderResult<Nothing>
}

/**
 * Moves device photos and videos into a real on-device folder under DCIM/<name>/ by updating each
 * MediaStore row's RELATIVE_PATH in place. [C] is the platform write-consent payload
 * (IntentSender in the Android binding).
 */
interface MoveToFolderUseCase<C> {
    suspend operator fun invoke(uris: List<String>, folderName: String): MoveToFolderResult<C>

    /** Replays the deferred move after the user granted the system write request. */
    suspend fun completeAfterPermissionGranted(pending: PendingMove): MoveToFolderResult<C>
}

/**
 * The MediaStore RELATIVE_PATH a move targets: DCIM/<sanitized name>/ with a trailing slash.
 * Sanitizes so the folder is a single safe path segment, and rejects a name that reduces to nothing.
 * Pure and Android-free so a JVM test can pin it.
 */
fun moveTargetRelativePath(folderName: String): String {
    val clean = folderName
        .trim()
        .replace('/', '_')
        .replace('\\', '_')
        .replace(':', '_')
        .replace(Regex("\\s+"), " ")
    require(clean.isNotEmpty()) { "folder name is blank after sanitizing" }
    return "DCIM/$clean/"
}
