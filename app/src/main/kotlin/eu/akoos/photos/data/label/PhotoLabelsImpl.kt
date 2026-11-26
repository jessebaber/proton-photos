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

package eu.akoos.photos.data.label

import eu.akoos.photos.data.db.dao.LabelBulkResult as DataBulk
import eu.akoos.photos.data.db.dao.LabelUsage as DataUsage
import eu.akoos.photos.domain.entity.GalleryItem
import eu.akoos.photos.domain.label.LabelBulkResult
import eu.akoos.photos.domain.label.LabelCreation
import eu.akoos.photos.domain.label.LabelUsage
import eu.akoos.photos.domain.label.PhotoLabels
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoLabelsImpl @Inject constructor(
    private val repo: PhotoLabelRepository,
) : PhotoLabels {
    override suspend fun currentUserKey(): String = repo.currentUserKey()
    override fun observeUserKey(): Flow<String> = repo.observeUserKey()
    override fun observeLabelsByPhoto(): Flow<Map<String, List<String>>> = repo.observeLabelsByPhoto()
    override fun observeLabelUsage(): Flow<List<LabelUsage>> =
        repo.observeLabelUsage().map { list -> list.map { LabelUsage(it.labelKey, it.label, it.count) } }
    override fun observeLocked(): Flow<Boolean> = repo.observeLocked()
    override suspend fun createLabel(text: String): LabelCreation = when (val c = repo.createLabel(text)) {
        is eu.akoos.photos.data.label.LabelCreation.Created -> LabelCreation.Created(c.label)
        is eu.akoos.photos.data.label.LabelCreation.Exists -> LabelCreation.Exists(c.label)
        eu.akoos.photos.data.label.LabelCreation.Invalid -> LabelCreation.Invalid
    }
    override suspend fun photoKeysOfLabel(label: String): Set<String> = repo.photoKeysOfLabel(label)
    override fun observeLabelsOf(photoKey: String): Flow<List<String>> = repo.observeLabelsOf(photoKey)
    override suspend fun labelsFor(item: GalleryItem): List<String> = repo.labelsFor(item)
    override suspend fun setLabels(item: GalleryItem, labels: List<String>) = repo.setLabels(item, labels)
    override suspend fun setLabels(photoKey: String, labels: List<String>) = repo.setLabels(photoKey, labels)
    override suspend fun addLabels(photoKeys: Collection<String>, labels: List<String>): LabelBulkResult {
        val r = repo.addLabels(photoKeys, labels)
        return LabelBulkResult(r.changedPhotos, r.cappedPhotos)
    }
    override suspend fun removeLabel(photoKey: String, label: String): Boolean = repo.removeLabel(photoKey, label)
    override suspend fun renameLabel(from: String, to: String): Boolean = repo.renameLabel(from, to)
    override suspend fun deleteLabels(labels: Collection<String>): Int = repo.deleteLabels(labels)
    override suspend fun deleteLabel(label: String): Int = repo.deleteLabel(label)
    override suspend fun rekey(oldKey: String, newKey: String): Int = repo.rekey(oldKey, newKey)
    override suspend fun deleteForPhotos(photoKeys: Collection<String>): Int = repo.deleteForPhotos(photoKeys)
    override suspend fun clearForUser(userId: UserId): Int = repo.clearForUser(userId)
    override suspend fun adoptGuestLabels(accountId: String): Int = repo.adoptGuestLabels(accountId)
    override suspend fun reconcileKeys(items: Iterable<GalleryItem>): Int = repo.reconcileKeys(items)
}
