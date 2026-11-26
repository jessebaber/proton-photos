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

package eu.akoos.photos.data.usecase

import eu.akoos.photos.domain.usecase.PersonNameSpelling

import eu.akoos.photos.domain.usecase.samePersonName

import android.content.Context
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.akoos.photos.data.db.dao.PersonDao
import eu.akoos.photos.data.db.entity.PersonEntity
import eu.akoos.photos.data.preferences.SettingsKeys
import eu.akoos.photos.data.preferences.settingsDataStore
import eu.akoos.photos.domain.usecase.AssignPersonNameUseCase
import eu.akoos.photos.domain.usecase.SameNamedPeopleMerge
import eu.akoos.photos.domain.usecase.personNameKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

internal fun sameNamedGroups(people: List<PersonEntity>): List<List<PersonEntity>> =
    people.filter { !it.isOther && !it.displayName.isNullOrBlank() }
        .groupBy { personNameKey(it.displayName!!) }
        .values.filter { it.size > 1 }
        .map { group -> group.sortedWith(compareByDescending<PersonEntity> { it.faceCount }.thenBy { it.id }) }

/**
 * Folds, once per account, the people an older build kept apart because their names differed only in case,
 * as the desktop always did, each into the one with the most faces under its spelling.
 */
@Singleton
class SameNamedPeopleMergeImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val personDao: PersonDao,
    private val assignPersonName: AssignPersonNameUseCase,
) : SameNamedPeopleMerge {
    override suspend fun runOnce(account: String) {
        if (account in context.settingsDataStore.data.first()[SettingsKeys.SAME_NAMED_PEOPLE_MERGED].orEmpty()) return
        for (group in sameNamedGroups(personDao.namedPeopleForUser(account))) {
            val keep = group.first().displayName!!.trim()
            for (other in group.drop(1)) assignPersonName(account, other.id, keep, confirm = true)
        }
        context.settingsDataStore.edit {
            it[SettingsKeys.SAME_NAMED_PEOPLE_MERGED] = it[SettingsKeys.SAME_NAMED_PEOPLE_MERGED].orEmpty() + account
        }
    }
}
