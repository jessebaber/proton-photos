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

import eu.akoos.photos.domain.entity.CloudPhoto

/**
 * Changes a single CLOUD photo's capture date and/or GPS place by replacing it with a corrected copy.
 * Android/Drive details stay in the data-layer implementation.
 */
interface WriteCloudPhotoMetadataUseCase {

    sealed interface Result {
        data object Success : Result

        /** Neither the date nor the place was addressed, so nothing was uploaded or trashed. */
        data object NothingToDo : Result

        /** [transient] marks a failure a retry could plausibly clear (network blip, 429/5xx) so the
         *  caller can hold the edit for another attempt rather than discard it. False by default: a
         *  failure not built from a classified network error stays permanent. */
        data class Failed(val reason: String, val transient: Boolean = false) : Result
    }

    suspend operator fun invoke(
        photo: CloudPhoto,
        newCaptureMs: Long?,
        location: LocationEdit,
        description: String? = null,
        artist: String? = null,
        copyright: String? = null,
        onPhase: ((CloudSavePhase) -> Unit)? = null,
        resumeUploadedLinkId: String? = null,
        onUploaded: (suspend (String) -> Unit)? = null,
    ): Result
}
