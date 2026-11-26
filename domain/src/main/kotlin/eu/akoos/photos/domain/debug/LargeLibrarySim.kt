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


package eu.akoos.photos.domain.debug

import kotlinx.coroutines.flow.StateFlow

data class LargeLibraryPopulateResult(val message: String)

data class DeviceSimState(
    val running: Boolean = false,
    val done: Int = 0,
    val total: Int = 0,
    val onDevice: Int = -1,
    val message: String? = null,
)

const val LARGE_LIBRARY_DEVICE_RELATIVE_PATH = "Pictures/Photos for Proton Sim"

/** DEBUG large-library cloud + device photo simulators. */
interface LargeLibrarySim {
    suspend fun populate(count: Int): LargeLibraryPopulateResult
    suspend fun clear(): LargeLibraryPopulateResult
    val deviceState: StateFlow<DeviceSimState>
    fun refreshDeviceCount()
    fun generateDevicePhotos(target: Int)
    fun removeDevicePhotos()
    fun cancelDeviceWork()
}
