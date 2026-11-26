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

package eu.akoos.photos.domain.vault

/** One photo on its way into the vault, before originals are deleted. */
data class VaultJournalEntry(
    val privateUri: String,
    val sourceUri: String,
    val sourceFolder: String? = null,
    val originalName: String? = null,
    val cloudLinkId: String? = null,
)

/**
 * Durable hide journal presentation drives: record intent, confirm after delete, discard on cancel,
 * reconcile leftovers, and probe free space before a hide.
 */
interface HiddenVaultJournalOps {
    suspend fun journal(entries: List<VaultJournalEntry>): Boolean
    suspend fun confirm(privateUris: List<String>): Int
    suspend fun discard(privateUris: List<String>)
    suspend fun reconcile()
    suspend fun spaceShortfallBytes(requiredBytes: Long): Long
}
