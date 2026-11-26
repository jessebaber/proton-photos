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

package eu.akoos.photos.data.upload.compression

import eu.akoos.photos.domain.compression.VideoCompressionPlanner as DomainPlanner
import eu.akoos.photos.domain.entity.compression.CompressionConditions
import eu.akoos.photos.domain.entity.compression.EncoderCapabilities
import eu.akoos.photos.domain.entity.compression.VideoCompressionPlan
import eu.akoos.photos.domain.entity.compression.VideoCompressionProfile
import eu.akoos.photos.domain.entity.compression.VideoSourceInfo

/** Forwards to the pure domain planner. */
object VideoCompressionPlanner {
    const val MAX_INPUT_BYTES = DomainPlanner.MAX_INPUT_BYTES
    const val MAX_INPUT_DURATION_MS = DomainPlanner.MAX_INPUT_DURATION_MS
    const val MIN_INPUT_DURATION_MS = DomainPlanner.MIN_INPUT_DURATION_MS
    const val MAX_INPUT_LONG_EDGE = DomainPlanner.MAX_INPUT_LONG_EDGE
    const val MIN_CLAMPED_SHORT_SIDE = DomainPlanner.MIN_CLAMPED_SHORT_SIDE
    const val TONE_MAP_MIN_SDK = DomainPlanner.TONE_MAP_MIN_SDK
    const val FREE_SPACE_MARGIN_BYTES = DomainPlanner.FREE_SPACE_MARGIN_BYTES

    fun plan(
        source: VideoSourceInfo,
        profile: VideoCompressionProfile,
        capabilities: EncoderCapabilities,
        conditions: CompressionConditions = CompressionConditions(),
    ): VideoCompressionPlan = DomainPlanner.plan(source, profile, capabilities, conditions)
}
