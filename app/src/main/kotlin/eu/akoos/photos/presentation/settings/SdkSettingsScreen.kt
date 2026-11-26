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

package eu.akoos.photos.presentation.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.akoos.photos.R
import eu.akoos.photos.domain.debug.SdkDebugRoute
import eu.akoos.photos.domain.debug.SdkDebugRoutes
import eu.akoos.photos.presentation.settings.components.RowDivider
import eu.akoos.photos.presentation.settings.components.SettingsCard
import eu.akoos.photos.presentation.settings.components.SettingsSubPageScaffold
import eu.akoos.photos.presentation.settings.components.ToggleRow
import eu.akoos.photos.presentation.theme.FgMute
import eu.akoos.photos.util.SdkDiagnostics
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Composable
fun SdkSettingsScreen(
    onBack: () -> Unit,
    viewModel: SdkSettingsViewModel = hiltViewModel(),
) {
    val switchedOn by viewModel.switchedOn.collectAsStateWithLifecycle()
    val activity by SdkDiagnostics.changes.collectAsStateWithLifecycle()
    val counts = remember(activity) {
        SdkDebugRoute.entries.associateWith { SdkDiagnostics.sdkCount(it.label) to SdkDiagnostics.fallbackCount(it.label) }
    }

    SettingsSubPageScaffold(title = stringResource(R.string.sdk_settings_title), onBack = onBack) {
        Text(
            stringResource(R.string.sdk_settings_intro),
            color = FgMute, fontSize = 12.sp, lineHeight = 17.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        SettingsCard {
            SdkDebugRoute.entries.forEachIndexed { index, route ->
                if (index > 0) RowDivider()
                ToggleRow(
                    label = stringResource(route.titleRes()),
                    description = stringResource(
                        R.string.sdk_route_activity,
                        stringResource(route.descriptionRes()),
                        counts.getValue(route).first,
                        counts.getValue(route).second,
                    ),
                    checked = route in switchedOn,
                    onCheckedChange = { viewModel.setSwitchedOn(route, it) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun SdkDebugRoute.titleRes(): Int = when (this) {
    SdkDebugRoute.TAGS -> R.string.sdk_route_tags
    SdkDebugRoute.TRASH -> R.string.sdk_route_trash
    SdkDebugRoute.DOWNLOAD -> R.string.sdk_route_download
    SdkDebugRoute.UPLOAD -> R.string.sdk_route_upload
    SdkDebugRoute.ALBUMS -> R.string.sdk_route_albums
    SdkDebugRoute.EVENTS -> R.string.sdk_route_events
    SdkDebugRoute.SHARED -> R.string.sdk_route_shared
}

private fun SdkDebugRoute.descriptionRes(): Int = when (this) {
    SdkDebugRoute.TAGS -> R.string.sdk_route_tags_desc
    SdkDebugRoute.TRASH -> R.string.sdk_route_trash_desc
    SdkDebugRoute.DOWNLOAD -> R.string.sdk_route_download_desc
    SdkDebugRoute.UPLOAD -> R.string.sdk_route_upload_desc
    SdkDebugRoute.ALBUMS -> R.string.sdk_route_albums_desc
    SdkDebugRoute.EVENTS -> R.string.sdk_route_events_desc
    SdkDebugRoute.SHARED -> R.string.sdk_route_shared_desc
}

@HiltViewModel
class SdkSettingsViewModel @Inject constructor(
    private val routing: SdkDebugRoutes,
) : ViewModel() {
    val switchedOn: StateFlow<Set<SdkDebugRoute>> =
        routing.switchedOn.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun setSwitchedOn(route: SdkDebugRoute, on: Boolean) {
        viewModelScope.launch { routing.setSwitchedOn(route, on) }
    }
}
