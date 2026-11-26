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


package eu.akoos.photos.data.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.akoos.photos.data.hidden.UserSignOut
import eu.akoos.photos.data.preferences.AccountScopedPreferences
import eu.akoos.photos.domain.settings.AccountCleanup
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountCleanupImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AccountCleanup {
    override suspend fun clearAccountPreferences(userId: UserId) {
        AccountScopedPreferences.clear(context, userId)
    }

    override fun markUserSignedOut(userId: String) {
        UserSignOut.mark(userId)
    }
}
