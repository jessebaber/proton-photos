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

import eu.akoos.photos.data.sdk.SdkRoute
import eu.akoos.photos.data.sdk.SdkRouting
import eu.akoos.photos.domain.debug.SdkDebugRoute
import eu.akoos.photos.domain.debug.SdkDebugRoutes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SdkDebugRoutesImpl @Inject constructor(
    private val routing: SdkRouting,
) : SdkDebugRoutes {
    override val isCapable: Boolean get() = routing.isCapable
    override val switchedOn: Flow<Set<SdkDebugRoute>> = routing.switchedOn.map { set ->
        set.mapNotNull { r -> runCatching { SdkDebugRoute.valueOf(r.name) }.getOrNull() }.toSet()
    }

    override suspend fun setSwitchedOn(route: SdkDebugRoute, on: Boolean) {
        routing.setSwitchedOn(SdkRoute.valueOf(route.name), on)
    }
}
