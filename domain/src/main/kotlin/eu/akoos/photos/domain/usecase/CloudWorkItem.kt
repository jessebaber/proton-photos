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

/** One resolved metadata replacement: [photo] rewritten with [newCaptureMs] (null leaves the date as it
 *  is), [location] (leave / clear / set), and the descriptive text tags [description] / [artist] /
 *  [copyright] (null leaves that tag unchanged, an empty string clears it). Resolved when the editor
 *  action fires, run on confirm. [deviceUri] is set for a SYNCED photo (a device file exists): the
 *  replacement uploads that already edited device file and re-pairs its sync row, rather than downloading
 *  the cloud original. Null means a cloud-only photo. */
data class CloudWorkItem(
    val photo: CloudPhoto,
    val newCaptureMs: Long?,
    val location: LocationEdit,
    val deviceUri: String? = null,
    val description: String? = null,
    val artist: String? = null,
    val copyright: String? = null,
)
