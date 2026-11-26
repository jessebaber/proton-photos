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

import eu.akoos.photos.domain.entity.SyncProgress
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId

/**
 * Pairs device files with Drive photos and updates sync state for one account.
 * MediaStore/DataStore/DAO work stays in the data-layer implementation.
 */
interface ReconcileSyncStateUseCase {
    /** Emits a running mark at once, then the finished totals of a pass that began after this call. */
    suspend operator fun invoke(userId: UserId): Flow<SyncProgress>
}
