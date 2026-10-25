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

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.akoos.photos.domain.entity.SyncStatus
import eu.akoos.photos.domain.repository.SyncStateRepository
import eu.akoos.photos.domain.usecase.FreeUpSpaceUseCase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.domain.AccountManager
import javax.inject.Inject

/**
 * Permanent-delete consent for free-up reclaim via [FreeUpPendingDeleteBridge]. Hard-separated from
 * trash / [eu.akoos.photos.data.preferences.SettingsKeys.PENDING_DELETE_URIS].
 */
@HiltViewModel
class FreeUpPermanentDeleteViewModel @Inject constructor(
    private val bridge: FreeUpPendingDeleteBridge,
    private val syncStateRepo: SyncStateRepository,
    private val freeUpSpace: FreeUpSpaceUseCase,
    private val accountManager: AccountManager,
) : ViewModel() {

    val pendingUris: StateFlow<Set<String>> = bridge.uris

    fun onConsentGranted(uris: Set<String>) {
        viewModelScope.launch {
            runCatching {
                syncStateRepo.updateStatusAndDeleteLocal(uris.toList(), SyncStatus.CLOUD_ONLY)
            }
            val userId = accountManager.getPrimaryUserId().first()
            if (userId != null) freeUpSpace.clearVerified(userId, uris.toList())
            bridge.remove(uris)
        }
    }

    fun onConsentDenied(uris: Set<String>) {
        viewModelScope.launch {
            val userId = accountManager.getPrimaryUserId().first()
            if (userId != null) freeUpSpace.clearVerified(userId, uris.toList())
            bridge.remove(uris)
        }
    }
}

@Composable
fun FreeUpPermanentDeleteHandler(
    viewModel: FreeUpPermanentDeleteViewModel = hiltViewModel(),
) {
    MediaStoreConsentHandler(
        pendingUris = viewModel.pendingUris,
        mode = MediaStoreConsentMode.PermanentDelete,
        settleDelayMs = 0L,
        onGranted = viewModel::onConsentGranted,
        onDenied = viewModel::onConsentDenied,
    )
}
