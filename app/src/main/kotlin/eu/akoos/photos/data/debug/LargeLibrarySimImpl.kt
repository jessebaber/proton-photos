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


package eu.akoos.photos.data.debug

import eu.akoos.photos.data.repository.drive.LargeLibraryDeviceSimulator
import eu.akoos.photos.data.repository.drive.LargeLibrarySimulator
import eu.akoos.photos.di.AppScope
import eu.akoos.photos.domain.debug.DeviceSimState
import eu.akoos.photos.domain.debug.LargeLibraryPopulateResult
import eu.akoos.photos.domain.debug.LargeLibrarySim
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LargeLibrarySimImpl @Inject constructor(
    private val simulator: LargeLibrarySimulator,
    private val deviceSimulator: LargeLibraryDeviceSimulator,
    @AppScope private val appScope: CoroutineScope,
) : LargeLibrarySim {
    override suspend fun populate(count: Int): LargeLibraryPopulateResult =
        LargeLibraryPopulateResult(simulator.populate(count).message)

    override suspend fun clear(): LargeLibraryPopulateResult =
        LargeLibraryPopulateResult("Cleared ${simulator.clear()} simulated photos")

    override val deviceState: StateFlow<DeviceSimState> = deviceSimulator.state
        .map {
            DeviceSimState(
                running = it.running,
                done = it.done,
                total = it.total,
                onDevice = it.onDevice,
                message = it.message,
            )
        }
        .stateIn(appScope, SharingStarted.Eagerly, DeviceSimState())

    override fun refreshDeviceCount() = deviceSimulator.refreshCount()
    override fun generateDevicePhotos(target: Int) = deviceSimulator.generate(target)
    override fun removeDevicePhotos() = deviceSimulator.removeAll()
    override fun cancelDeviceWork() = deviceSimulator.cancel()
}
