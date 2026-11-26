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

package eu.akoos.photos.domain.ml

import java.io.File

sealed interface FaceModelPreparation {
    data class Ready(val file: File) : FaceModelPreparation
    data object NeedsConsent : FaceModelPreparation
    data class Failed(val reason: String) : FaceModelPreparation
}

sealed interface OcrModelPreparation {
    data object Ready : OcrModelPreparation
    data object NeedsConsent : OcrModelPreparation
    data object Failed : OcrModelPreparation
}

sealed interface SemanticModelPreparation {
    data class Ready(val files: Map<String, File>) : SemanticModelPreparation
    data object NeedsConsent : SemanticModelPreparation
    data class Failed(val reason: String) : SemanticModelPreparation
}

/** Face recognition model on-disk state and download preparation. */
interface FaceModels {
    suspend fun onDisk(): File?
    suspend fun prepare(
        onDownloadStart: () -> Unit = {},
        onProgress: (Long) -> Unit = {},
    ): FaceModelPreparation
    suspend fun deleteAll()
}

/** OCR model on-disk state and download preparation. */
interface OcrModels {
    fun filesPresentQuick(): Boolean
    suspend fun ensure(
        onDownloadStart: () -> Unit = {},
    ): OcrModelPreparation
    suspend fun deleteAll()
}

/** Semantic embedding model on-disk state and download preparation. */
interface SemanticModels {
    suspend fun onDisk(): Map<String, File>?
    suspend fun prepare(
        onDownloadStart: () -> Unit = {},
        onProgress: (Long) -> Unit = {},
    ): SemanticModelPreparation
    suspend fun deleteAll()
}
