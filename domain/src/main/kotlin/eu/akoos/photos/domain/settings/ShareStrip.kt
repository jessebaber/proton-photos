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

package eu.akoos.photos.domain.settings

/** Share-time metadata strip choices produced from SettingsStore. */
data class ShareStripConfig(
    val stripGps: Boolean = false,
    val stripCameraInfo: Boolean = false,
    val stripTimestamp: Boolean = false,
    val stripSoftwareInfo: Boolean = false,
    val stripAuthorship: Boolean = false,
)

/** Null when STRIP_ON_SHARE is off; otherwise the per-field share strip choices. */
suspend fun SettingsStore.currentShareStripConfig(): ShareStripConfig? {
    if (!get(Settings.STRIP_ON_SHARE)) return null
    return ShareStripConfig(
        stripGps = get(Settings.STRIP_SHARE_GPS),
        stripCameraInfo = get(Settings.STRIP_SHARE_CAMERA_INFO),
        stripTimestamp = get(Settings.STRIP_SHARE_TIMESTAMP),
        stripSoftwareInfo = get(Settings.STRIP_SHARE_SOFTWARE_INFO),
        stripAuthorship = get(Settings.STRIP_SHARE_AUTHORSHIP),
    )
}

