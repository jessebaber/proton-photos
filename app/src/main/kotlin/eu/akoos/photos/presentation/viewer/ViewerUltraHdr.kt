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


package eu.akoos.photos.presentation.viewer

import coil.request.ImageRequest

/** Log tag shared with the Ultra HDR decode path. */
const val ULTRA_HDR_TAG = "UltraHdr"

/**
 * Opts this request into the Ultra HDR decode path (same parameter the data-layer decoder reads).
 * Lives in presentation so the viewer never imports data.image.
 */
fun ImageRequest.Builder.decodeUltraHdr(): ImageRequest.Builder =
    setParameter(key = "eu.akoos.photos.ultra_hdr", value = true, memoryCacheKey = null)
