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

package eu.akoos.photos.data.ml

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.akoos.photos.data.face.FaceModelManager
import eu.akoos.photos.data.face.FaceModelPreparation as DataFacePrep
import eu.akoos.photos.data.ocr.OcrModelManager
import eu.akoos.photos.data.ocr.OnnxTextRecognizer
import eu.akoos.photos.data.semantic.SemanticModelManager
import eu.akoos.photos.data.semantic.SemanticModelPreparation as DataSemanticPrep
import eu.akoos.photos.domain.ml.FaceModelPreparation
import eu.akoos.photos.domain.ml.FaceModels
import eu.akoos.photos.domain.ml.OcrModelPreparation
import eu.akoos.photos.domain.ml.OcrModels
import eu.akoos.photos.domain.ml.SemanticModelPreparation
import eu.akoos.photos.domain.ml.SemanticModels
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaceModelsImpl @Inject constructor(
    @ApplicationContext context: Context,
) : FaceModels {
    private val manager = FaceModelManager(context)

    override suspend fun onDisk(): File? = manager.onDisk()
    override suspend fun prepare(
        onDownloadStart: () -> Unit,
        onProgress: (Long) -> Unit,
    ): FaceModelPreparation = when (val prep = manager.prepare(onDownloadStart, onProgress)) {
        is DataFacePrep.Ready -> FaceModelPreparation.Ready(prep.file)
        DataFacePrep.NeedsConsent -> FaceModelPreparation.NeedsConsent
        is DataFacePrep.Failed -> FaceModelPreparation.Failed(prep.reason)
    }
    override suspend fun deleteAll() = manager.deleteAll()
}

@Singleton
class OcrModelsImpl @Inject constructor(
    @ApplicationContext context: Context,
) : OcrModels {
    private val manager = OcrModelManager(context)
    private val recognizer = OnnxTextRecognizer(context)

    override fun filesPresentQuick(): Boolean = manager.filesPresentQuick()
    override suspend fun ensure(onDownloadStart: () -> Unit): OcrModelPreparation =
        when (recognizer.prepare(onDownloadStart)) {
            eu.akoos.photos.data.ocr.OcrModelPreparation.Ready -> OcrModelPreparation.Ready
            eu.akoos.photos.data.ocr.OcrModelPreparation.NeedsConsent -> OcrModelPreparation.NeedsConsent
            eu.akoos.photos.data.ocr.OcrModelPreparation.Failed -> OcrModelPreparation.Failed
        }
    override suspend fun deleteAll() = manager.deleteAll()
}

@Singleton
class SemanticModelsImpl @Inject constructor(
    @ApplicationContext context: Context,
) : SemanticModels {
    private val manager = SemanticModelManager(context)

    override suspend fun onDisk(): Map<String, File>? =
        if (manager.areModelsPresent()) emptyMap() else null

    override suspend fun prepare(
        onDownloadStart: () -> Unit,
        onProgress: (Long) -> Unit,
    ): SemanticModelPreparation = when (val prep = manager.ensureDownloaded(onDownloadStart, onProgress)) {
        is DataSemanticPrep.Ready -> SemanticModelPreparation.Ready(
            mapOf("image" to prep.files.image, "text" to prep.files.text),
        )
        DataSemanticPrep.NeedsConsent -> SemanticModelPreparation.NeedsConsent
        is DataSemanticPrep.Failed -> SemanticModelPreparation.Failed(prep.reason)
    }
    override suspend fun deleteAll() = manager.deleteAll()
}
