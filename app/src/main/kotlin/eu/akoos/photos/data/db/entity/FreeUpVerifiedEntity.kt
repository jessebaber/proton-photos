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

package eu.akoos.photos.data.db.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * One Drive-verified free-up candidate. Separate from `sync_state` so upserts of the sync row cannot
 * wipe the verify stamp. Joined back on [localUri] + matching [cloudFileId] for the same [userId].
 *
 * Stub entity for Phase 6 contract tests; registered on AppDatabase in Phase 7.
 */
@Entity(
    tableName = "free_up_verified",
    primaryKeys = ["userId", "localUri"],
    indices = [Index("userId"), Index("localUri")],
)
data class FreeUpVerifiedEntity(
    val userId: String,
    val localUri: String,
    val cloudFileId: String,
    val verifiedAtMs: Long,
)
