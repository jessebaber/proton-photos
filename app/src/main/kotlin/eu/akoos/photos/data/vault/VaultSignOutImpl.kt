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

package eu.akoos.photos.data.vault

import eu.akoos.photos.data.hidden.HiddenRescue
import eu.akoos.photos.data.hidden.HiddenVaultRestorer
import eu.akoos.photos.data.hidden.VaultSignOutPolicy
import eu.akoos.photos.data.mlsync.MlSyncSignOut
import eu.akoos.photos.domain.repository.VaultWipe
import eu.akoos.photos.domain.vault.VaultRescueResult
import eu.akoos.photos.domain.vault.VaultSignOut
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultSignOutImpl @Inject constructor(
    private val restorer: HiddenVaultRestorer,
    private val mlSyncSignOut: MlSyncSignOut,
) : VaultSignOut {
    override val signOutFolder: String get() = HiddenRescue.SIGN_OUT_FOLDER

    override suspend fun rescueDeviceOnly(folderName: String): VaultRescueResult? =
        runCatching { restorer.rescueDeviceOnly(folderName) }.getOrNull()?.let {
            VaultRescueResult(it.moved, it.stillInVault, it.zipName)
        }

    override fun vaultNote(rescued: VaultRescueResult?): String {
        val data = rescued?.let { HiddenRescue(it.moved, it.stillInVault, it.zipName) }
        return VaultSignOutPolicy.vaultNote(data)
    }

    override fun wipeAfterRescue(rescued: VaultRescueResult?): VaultWipe {
        val data = rescued?.let { HiddenRescue(it.moved, it.stillInVault, it.zipName) }
        return VaultSignOutPolicy.wipeAfterRescue(data)
    }

    override suspend fun stopMlSyncFor(userId: UserId) {
        mlSyncSignOut.stopFor(userId)
    }
}
