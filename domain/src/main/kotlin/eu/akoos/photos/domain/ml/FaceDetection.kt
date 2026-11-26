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

/** One face a detector found, in the frame's own pixel coordinates. */
data class DetectedFaceBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val score: Float,
    /** Flattened landmark pairs x0,y0,x1,y1,... in detector order. */
    val landmarks: FloatArray = floatArrayOf(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DetectedFaceBox) return false
        return left == other.left && top == other.top && right == other.right &&
            bottom == other.bottom && score == other.score && landmarks.contentEquals(other.landmarks)
    }

    override fun hashCode(): Int {
        var r = left.hashCode()
        r = 31 * r + top.hashCode()
        r = 31 * r + right.hashCode()
        r = 31 * r + bottom.hashCode()
        r = 31 * r + score.hashCode()
        r = 31 * r + landmarks.contentHashCode()
        return r
    }
}

/** Runs face detection on one frame. Frame type [F] is bound by the implementation. */
interface FaceDetectorEngine<F> : AutoCloseable {
    suspend fun detect(frame: F): List<DetectedFaceBox>
}

/** Opens per-use detector sessions so presentation never imports the ONNX implementation. */
interface FaceDetectorFactory {
    fun <F> open(modelFile: File, scoreThreshold: Float = DEFAULT_SCORE): FaceDetectorEngine<F>

    companion object {
        const val DEFAULT_SCORE = 0.6f
    }
}
