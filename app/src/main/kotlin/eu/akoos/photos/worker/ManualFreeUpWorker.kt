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

package eu.akoos.photos.worker

import android.content.Context
import androidx.work.Constraints

/**
 * Manual Free-up entry points: enqueue/cancel the shared [FreeUpPipelineWorker] under a distinct
 * unique name (expedited, network-only — no AUTO_FREE_UP / thermal gates).
 *
 * Stop cancels [UNIQUE_NAME] only and does not clear the automatic mode's verified markers.
 */
object ManualFreeUpWorker {
    const val TAG = "manual_free_up_worker"
    const val UNIQUE_NAME = "manual_free_up"

    /** @see FreeUpPipelineWorker.KEY_DONE */
    const val KEY_DONE = FreeUpPipelineWorker.KEY_DONE

    /** @see FreeUpPipelineWorker.KEY_TOTAL */
    const val KEY_TOTAL = FreeUpPipelineWorker.KEY_TOTAL

    /** @see FreeUpPipelineWorker.KEY_PROCESSED */
    const val KEY_READY = "ready"

    /** Constraints for the manual pipeline: network required; no AUTO_FREE_UP / thermal. */
    fun verifyConstraints(): Constraints = FreeUpPipelineWorker.manualConstraints()

    fun enqueue(context: Context) {
        FreeUpPipelineWorker.enqueueManual(context)
    }

    fun cancel(context: Context) {
        FreeUpPipelineWorker.cancelManual(context)
    }
}
