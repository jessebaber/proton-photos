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

enum class GuestFaceMigration { NONE, ADOPT, DISCARD }

/**
 * The outcome of a sign-in migration: the [action] taken and how many people and face rows it carried
 * across (ADOPT) or dropped (DISCARD), so the copyable diagnostics can show what the sign-in did. Both
 * counts are 0 for NONE.
 */
data class GuestFaceMigrationResult(
    val action: GuestFaceMigration,
    val people: Int,
    val faces: Int,
)

/**
 * Decides the fate of guest ("local") face data on sign-in. A guest scans device photos and builds
 * People + names under the `LOCAL_USER` owner; every face table keys its rows by that owner. Without
 * this step, signing in re-scans the same device files under the account and the re-scan overwrites
 * each guest face row on its shared primary key with a nameless one, silently destroying the names.
 *
 * - No guest data -> NONE.
 * - Guest data AND the account has none yet -> ADOPT: re-key every table `local` -> account, so the
 *   people, names, memberships, scan markers and curation carry over and the account does not re-scan.
 * - Guest data BUT the account already has its own faces -> DISCARD: the account owns the People it
 *   already built, and the `face` table is single-row-per-photo so the two cannot be merged; drop the
 *   guest rows so stale biometric vectors do not linger under the signed-in session.
 */
fun guestFaceMigrationAction(hasGuestData: Boolean, accountHasData: Boolean): GuestFaceMigration =
    when {
        !hasGuestData -> GuestFaceMigration.NONE
        accountHasData -> GuestFaceMigration.DISCARD
        else -> GuestFaceMigration.ADOPT
    }
