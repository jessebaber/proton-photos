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

import android.graphics.Bitmap
import eu.akoos.photos.data.face.DetectedFace
import eu.akoos.photos.data.face.FaceDetector
import eu.akoos.photos.domain.ml.DetectedFaceBox
import eu.akoos.photos.domain.ml.FaceDetectorEngine
import eu.akoos.photos.domain.ml.FaceDetectorFactory
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaceDetectorFactoryImpl @Inject constructor() : FaceDetectorFactory {
    @Suppress("UNCHECKED_CAST")
    override fun <F> open(modelFile: File, scoreThreshold: Float): FaceDetectorEngine<F> =
        Engine(FaceDetector(modelFile, scoreThreshold = scoreThreshold)) as FaceDetectorEngine<F>

    private class Engine(
        private val detector: FaceDetector,
    ) : FaceDetectorEngine<Bitmap> {
        override suspend fun detect(frame: Bitmap): List<DetectedFaceBox> =
            detector.detect(frame).map { it.toDomain() }

        override fun close() = detector.close()
    }
}

private fun DetectedFace.toDomain(): DetectedFaceBox {
    val flat = FloatArray(landmarks.size * 2)
    landmarks.forEachIndexed { i, p ->
        flat[i * 2] = p.x
        flat[i * 2 + 1] = p.y
    }
    return DetectedFaceBox(
        left = box.left,
        top = box.top,
        right = box.right,
        bottom = box.bottom,
        score = score,
        landmarks = flat,
    )
}
