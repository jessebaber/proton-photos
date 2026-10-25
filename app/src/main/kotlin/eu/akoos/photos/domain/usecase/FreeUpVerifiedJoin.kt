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

package eu.akoos.photos.domain.usecase

/**
 * Pure join rules for a `sync_state` row paired with a `free_up_verified` row. Mirrors the SQL the
 * ready-to-reclaim query uses so a JVM test can pin the safety gates without Room.
 *
 * A row is reclaimable only when the verified marker still matches the live sync pairing for the
 * same account, the sync row is still SYNCED, and the verify stamp is fresh enough.
 */
fun matchesFreeUpVerifiedJoin(
    syncUserId: String,
    queryUserId: String,
    syncStatus: String,
    syncCloudFileId: String?,
    verifiedUserId: String,
    verifiedCloudFileId: String,
    verifiedAtMs: Long,
    sinceMs: Long,
): Boolean =
    syncUserId == queryUserId &&
        verifiedUserId == queryUserId &&
        syncStatus == "SYNCED" &&
        !syncCloudFileId.isNullOrBlank() &&
        syncCloudFileId == verifiedCloudFileId &&
        verifiedAtMs >= sinceMs
