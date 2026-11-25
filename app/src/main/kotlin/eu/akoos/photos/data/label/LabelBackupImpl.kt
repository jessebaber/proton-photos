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

import eu.akoos.photos.di.AppScope
import eu.akoos.photos.domain.label.LabelBackup
import eu.akoos.photos.domain.label.LabelBackupCopies
import eu.akoos.photos.domain.label.LabelBackupStart
import eu.akoos.photos.domain.label.LabelBackupUiState
import eu.akoos.photos.domain.label.LabelSavedCopy as DomainCopy
import eu.akoos.photos.data.label.LabelBackupResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LabelBackupImpl @Inject constructor(
    private val controller: LabelBackupController,
    @AppScope private val appScope: CoroutineScope,
) : LabelBackup {
    override val state: StateFlow<LabelBackupUiState> = controller.state
        .map { it.toDomain() }
        .stateIn(appScope, SharingStarted.Eagerly, LabelBackupUiState.Idle)

    override fun isAvailable(userId: UserId?): Boolean = controller.isAvailable(userId)

    override suspend fun save(userId: UserId, createFolderConfirmed: Boolean): LabelBackupStart =
        controller.save(userId, createFolderConfirmed).toDomain()

    override suspend fun listCopies(userId: UserId): LabelBackupCopies =
        when (val copies = controller.listCopies(userId)) {
            is LabelBackupRunner.Copies.Found -> LabelBackupCopies.Found(copies.copies.map { it.toDomain() })
            LabelBackupRunner.Copies.Empty -> LabelBackupCopies.Empty
            LabelBackupRunner.Copies.NotCapable -> LabelBackupCopies.NotCapable
            LabelBackupRunner.Copies.Failed -> LabelBackupCopies.Failed
        }

    override suspend fun restore(userId: UserId, copy: DomainCopy): LabelBackupStart {
        val dataCopy = when (val listed = controller.listCopies(userId)) {
            is LabelBackupRunner.Copies.Found -> listed.copies.firstOrNull {
                it.deviceId == copy.deviceId && it.revisionUid == copy.revisionUid
            }
            else -> null
        } ?: return LabelBackupStart.NOT_AVAILABLE
        return controller.restore(userId, dataCopy).toDomain()
    }

    override fun cancel() = controller.cancel()
    override fun dismissResult() = controller.dismissResult()

    private fun LabelBackupState.toDomain(): LabelBackupUiState = when (this) {
        LabelBackupState.Idle -> LabelBackupUiState.Idle
        is LabelBackupState.Running -> LabelBackupUiState.Running(
            operation = operation.name,
            step = progress.step.name,
        )
        is LabelBackupState.Finished -> LabelBackupUiState.Finished(
            ok = outcome.result == LabelBackupResult.SAVED ||
                outcome.result == LabelBackupResult.RESTORED ||
                outcome.result == LabelBackupResult.NOTHING_TO_SAVE,
            result = outcome.result.name,
        )
    }

    private fun eu.akoos.photos.data.label.LabelBackupStart.toDomain() = when (this) {
        eu.akoos.photos.data.label.LabelBackupStart.STARTED -> LabelBackupStart.STARTED
        eu.akoos.photos.data.label.LabelBackupStart.ALREADY_RUNNING -> LabelBackupStart.ALREADY_RUNNING
        eu.akoos.photos.data.label.LabelBackupStart.NOT_AVAILABLE -> LabelBackupStart.NOT_AVAILABLE
        eu.akoos.photos.data.label.LabelBackupStart.FOLDER_MISSING -> LabelBackupStart.FOLDER_MISSING
    }

    private fun LabelSavedCopy.toDomain() = DomainCopy(
        deviceId = deviceId,
        deviceLabel = deviceLabel,
        isThisDevice = isThisDevice,
        savedAtMillis = savedAtMillis,
        revisionUid = revisionUid,
        labelCount = labels.size,
    )
}
