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

import eu.akoos.photos.data.hidden.HiddenFolderRecords
import eu.akoos.photos.data.hidden.HiddenVaultDiagnostics
import eu.akoos.photos.domain.vault.VaultDiagnostics
import eu.akoos.photos.domain.vault.VaultHideSplit
import eu.akoos.photos.domain.vault.VaultHideTarget
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultDiagnosticsImpl @Inject constructor() : VaultDiagnostics {
    override fun hideStarted(split: VaultHideSplit) =
        HiddenVaultDiagnostics.hideStarted(split.toData())

    override fun hideStarted(vaultedCount: Int, pairedCount: Int, filteredCount: Int, totalBytes: Long) =
        HiddenVaultDiagnostics.hideStarted(vaultedCount, pairedCount, filteredCount, totalBytes)

    override fun spaceChecked(requiredBytes: Long, shortfallBytes: Long) =
        HiddenVaultDiagnostics.spaceChecked(requiredBytes, shortfallBytes)

    override fun copied(copiedCount: Int, failedCount: Int) =
        HiddenVaultDiagnostics.copied(copiedCount, failedCount)

    override fun originalsRemoved(count: Int, neededConsent: Boolean) =
        HiddenVaultDiagnostics.originalsRemoved(count, neededConsent)

    override fun originalsAwaitingConsent(count: Int) =
        HiddenVaultDiagnostics.originalsAwaitingConsent(count)

    private fun VaultHideSplit.toData() = HiddenFolderRecords.HideSplit(
        vaultable = vaultable.map {
            HiddenFolderRecords.VaultTarget(it.item, it.local, it.cloudLinkId, it.sizeBytes)
        },
        cloudLinkIds = cloudLinkIds,
    )
}
