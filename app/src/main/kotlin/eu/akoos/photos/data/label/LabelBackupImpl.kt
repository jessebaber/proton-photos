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
import eu.akoos.photos.domain.label.LabelBackupOperation as DomOp
import eu.akoos.photos.domain.label.LabelBackupOutcome as DomOutcome
import eu.akoos.photos.domain.label.LabelBackupProgress as DomProgress
import eu.akoos.photos.domain.label.LabelBackupResult as DomResult
import eu.akoos.photos.domain.label.LabelBackupStart as DomStart
import eu.akoos.photos.domain.label.LabelBackupState as DomState
import eu.akoos.photos.domain.label.LabelBackupStep as DomStep
import eu.akoos.photos.domain.label.LabelCount as DomCount
import eu.akoos.photos.domain.label.LabelSavedCopy as DomCopy
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
    override val state: StateFlow<DomState> = controller.state
        .map { it.toDomain() }
        .stateIn(appScope, SharingStarted.Eagerly, DomState.Idle)

    override fun isAvailable(userId: UserId?): Boolean = controller.isAvailable(userId)

    override suspend fun save(userId: UserId, createFolderConfirmed: Boolean): DomStart =
        controller.save(userId, createFolderConfirmed).toDomain()

    override suspend fun listCopies(userId: UserId): LabelBackupCopies =
        when (val copies = controller.listCopies(userId)) {
            is LabelBackupRunner.Copies.Found -> LabelBackupCopies.Found(copies.copies.map { it.toDomain() })
            LabelBackupRunner.Copies.Empty -> LabelBackupCopies.Empty
            LabelBackupRunner.Copies.NotCapable -> LabelBackupCopies.NotCapable
            LabelBackupRunner.Copies.Failed -> LabelBackupCopies.Failed
        }

    override suspend fun restore(userId: UserId, copy: DomCopy): DomStart {
        val dataCopy = when (val listed = controller.listCopies(userId)) {
            is LabelBackupRunner.Copies.Found -> listed.copies.firstOrNull {
                it.deviceId == copy.deviceId && it.revisionUid == copy.revisionUid
            }
            else -> null
        } ?: return DomStart.NOT_AVAILABLE
        return controller.restore(userId, dataCopy).toDomain()
    }

    override fun cancel() = controller.cancel()
    override fun dismissResult() = controller.dismissResult()

    private fun LabelBackupState.toDomain(): DomState = when (this) {
        LabelBackupState.Idle -> DomState.Idle
        is LabelBackupState.Running -> DomState.Running(
            operation = DomOp.valueOf(operation.name),
            progress = DomProgress(DomStep.valueOf(progress.step.name)),
        )
        is LabelBackupState.Finished -> DomState.Finished(
            runId = runId,
            outcome = DomOutcome(
                operation = DomOp.valueOf(outcome.operation.name),
                result = DomResult.valueOf(outcome.result.name),
                photos = outcome.photos,
                newLabels = outcome.newLabels,
            ),
        )
    }

    private fun LabelBackupStart.toDomain() = when (this) {
        LabelBackupStart.STARTED -> DomStart.STARTED
        LabelBackupStart.ALREADY_RUNNING -> DomStart.ALREADY_RUNNING
        LabelBackupStart.NOT_AVAILABLE -> DomStart.NOT_AVAILABLE
        LabelBackupStart.FOLDER_MISSING -> DomStart.FOLDER_MISSING
    }

    private fun LabelSavedCopy.toDomain() = DomCopy(
        deviceId = deviceId,
        deviceLabel = deviceLabel,
        isThisDevice = isThisDevice,
        savedAtMillis = savedAtMillis,
        revisionUid = revisionUid,
        labels = labels.map { DomCount(it.label, it.photos) },
    )
}
