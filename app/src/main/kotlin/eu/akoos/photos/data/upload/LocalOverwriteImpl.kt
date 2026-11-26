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

package eu.akoos.photos.data.upload

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.akoos.photos.domain.upload.LocalOverwrite
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalOverwriteImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocalOverwrite {
    override fun <T> run(
        uri: String,
        expectedWidth: Int?,
        expectedHeight: Int?,
        mutate: () -> T,
    ): T {
        val expected = if (expectedWidth != null && expectedHeight != null) {
            expectedWidth to expectedHeight
        } else null
        return LocalOverwriteGuard(context).run(uri, expectedSize = expected, mutate = mutate)
    }
}
