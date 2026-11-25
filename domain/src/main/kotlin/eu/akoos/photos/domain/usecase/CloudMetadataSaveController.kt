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

import kotlinx.coroutines.flow.StateFlow

/**
 * App-scoped owner of the cloud metadata save batch. The editor snapshots its staged edits and hands
 * them here; the data-layer implementation persists them and drains the corrected copies in the
 * background. [ui] is the live view a progress drawer reads.
 */
interface CloudMetadataSaveController {

    /** What a progress surface shows for the current batch. Null (see [ui]) means nothing to show,
     *  either never started or dismissed. */
    sealed interface SaveUi {
        data class Running(val done: Int, val total: Int, val failed: Int, val phase: CloudSavePhase) : SaveUi
        data class Done(val updated: Int, val failed: Int) : SaveUi
    }

    /** The drawer's live view of the current batch. */
    val ui: StateFlow<SaveUi?>

    /** Persists [work] and starts the drain. [localUpdated] counts device edits already written in place,
     *  folded into the drawer's totals through [ui]. */
    fun start(work: List<CloudWorkItem>, localUpdated: Int = 0)

    /** Hides the progress surface without stopping the drain. */
    fun dismiss()

    /** Stops the drain and hides the progress surface at once. */
    fun cancel()
}
