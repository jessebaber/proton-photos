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

import eu.akoos.photos.data.hidden.HiddenVaultJournal
import eu.akoos.photos.domain.vault.HiddenVaultJournalOps
import eu.akoos.photos.domain.vault.VaultJournalEntry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HiddenVaultJournalOpsImpl @Inject constructor(
    private val journal: HiddenVaultJournal,
) : HiddenVaultJournalOps {
    override suspend fun journal(entries: List<VaultJournalEntry>): Boolean =
        journal.journal(entries.map {
            HiddenVaultJournal.Entry(
                privateUri = it.privateUri,
                sourceUri = it.sourceUri,
                sourceFolder = it.sourceFolder,
                originalName = it.originalName,
                cloudLinkId = it.cloudLinkId,
            )
        })

    override suspend fun confirm(privateUris: List<String>): Int = journal.confirm(privateUris)
    override suspend fun discard(privateUris: List<String>) = journal.discard(privateUris)
    override suspend fun reconcile() = journal.reconcile()
    override suspend fun spaceShortfallBytes(requiredBytes: Long): Long =
        journal.spaceShortfallBytes(requiredBytes)
}
