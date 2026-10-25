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

package eu.akoos.photos.presentation.settings

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.akoos.photos.R
import eu.akoos.photos.domain.entity.SyncState
import eu.akoos.photos.domain.usecase.FreeUpSpaceUseCase
import eu.akoos.photos.worker.ManualFreeUpWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.domain.AccountManager
import javax.inject.Inject

/**
 * Free up space screen: lists reclaimable candidates, enqueues [ManualFreeUpWorker] for background
 * Drive verify, observes WorkInfo + ready count, and runs the foreground finisher (live re-check
 * then permanent delete). Leaving the screen no longer cancels verify.
 */
@HiltViewModel
class FreeUpSpaceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val freeUpSpace: FreeUpSpaceUseCase,
    private val localRepo: eu.akoos.photos.domain.repository.LocalMediaRepository,
    private val accountManager: AccountManager,
    private val pendingDeleteBridge: FreeUpPendingDeleteBridge,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val candidates: List<SyncState> = emptyList(),
        val phase: FreeUpSpacePhase = FreeUpSpacePhase.Idle,
        val checked: Int = 0,
        val checkTotal: Int = 0,
        val readyCount: Int = 0,
        val message: String? = null,
        val reclaimableBytes: Long = 0L,
    ) {
        val isEmpty: Boolean get() = !loading && candidates.isEmpty() && readyCount == 0
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var finishStartedForSuccess = false

    init {
        load()
        observeWorker()
    }

    fun load() {
        viewModelScope.launch {
            val userId = accountManager.getPrimaryUserId().first() ?: run {
                _uiState.update { it.copy(loading = false, candidates = emptyList()) }
                return@launch
            }
            _uiState.update { it.copy(loading = true) }
            val rows = freeUpSpace.candidates(userId, Long.MAX_VALUE, protectDownloaded = false)
            val ready = freeUpSpace.readyToReclaim(userId)
            _uiState.update {
                it.copy(
                    loading = false,
                    candidates = rows,
                    readyCount = ready.size,
                    reclaimableBytes = totalBytes(rows),
                )
            }
        }
    }

    /** Enqueue background verify for the listed candidates. */
    fun start() {
        if (_uiState.value.phase == FreeUpSpacePhase.Checking) return
        finishStartedForSuccess = false
        ManualFreeUpWorker.enqueue(context)
    }

    /** Foreground finish: live Drive re-check then permanent delete. */
    fun finishReclaim() {
        if (_uiState.value.phase == FreeUpSpacePhase.Freeing) return
        viewModelScope.launch {
            val userId = accountManager.getPrimaryUserId().first() ?: return@launch
            _uiState.update { it.copy(phase = FreeUpSpacePhase.Freeing, message = null) }
            try {
                val confirmed = freeUpSpace.confirmReadyForReclaim(userId)
                if (confirmed.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            phase = FreeUpSpacePhase.Done,
                            readyCount = 0,
                            message = context.getString(R.string.settings_free_up_none),
                        )
                    }
                    load()
                    return@launch
                }
                if (Environment.isExternalStorageManager()) {
                    val result = freeUpSpace.reclaimCandidates(confirmed) { done, total ->
                        _uiState.update { it.copy(checked = done, checkTotal = total) }
                    }
                    handleReclaimResult(userId, result)
                } else {
                    // Direct deletes may free some; NeedsPermission URIs go to the permanent-delete
                    // handler (never trash / PENDING_DELETE_URIS).
                    val result = freeUpSpace.reclaimCandidates(confirmed) { done, total ->
                        _uiState.update { it.copy(checked = done, checkTotal = total) }
                    }
                    handleReclaimResult(userId, result)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update {
                    it.copy(
                        phase = FreeUpSpacePhase.Error,
                        message = context.getString(R.string.settings_free_up_error, e.message ?: ""),
                    )
                }
            }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    private fun observeWorker() {
        viewModelScope.launch {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkFlow(ManualFreeUpWorker.UNIQUE_NAME)
                .collect { infos ->
                    val info = infos.firstOrNull()
                    val running = info?.state == WorkInfo.State.RUNNING ||
                        info?.state == WorkInfo.State.ENQUEUED ||
                        info?.state == WorkInfo.State.BLOCKED
                    val succeeded = info?.state == WorkInfo.State.SUCCEEDED
                    val failed = info?.state == WorkInfo.State.FAILED
                    val progress = info?.progress
                    val checked = progress?.getInt(ManualFreeUpWorker.KEY_DONE, 0) ?: 0
                    val total = progress?.getInt(ManualFreeUpWorker.KEY_TOTAL, 0) ?: 0

                    val userId = accountManager.getPrimaryUserId().first()
                    val readyCount = if (userId != null) {
                        freeUpSpace.readyToReclaim(userId).size
                    } else {
                        0
                    }
                    val current = _uiState.value
                    val freeing = current.phase == FreeUpSpacePhase.Freeing
                    val doneMessage = current.message != null && current.phase == FreeUpSpacePhase.Done
                    val phase = deriveFreeUpSpaceState(
                        workRunning = running,
                        workSucceeded = succeeded,
                        workFailed = failed,
                        readyCount = readyCount,
                        freeing = freeing,
                        doneMessage = doneMessage,
                    )
                    _uiState.update {
                        it.copy(
                            phase = if (freeing) FreeUpSpacePhase.Freeing else phase,
                            checked = checked,
                            checkTotal = total,
                            readyCount = readyCount,
                        )
                    }
                    // Auto-start reclaim when verify succeeds and the screen is open.
                    if (succeeded && readyCount > 0 && !finishStartedForSuccess && !freeing) {
                        finishStartedForSuccess = true
                        finishReclaim()
                    }
                }
        }
    }

    private suspend fun handleReclaimResult(
        userId: me.proton.core.domain.entity.UserId,
        result: FreeUpSpaceUseCase.FreeUpResult,
    ) {
        when (result) {
            is FreeUpSpaceUseCase.FreeUpResult.Done -> {
                freeUpSpace.clearVerified(userId)
                _uiState.update {
                    it.copy(phase = FreeUpSpacePhase.Done, message = freedMessage(result.freed), readyCount = 0)
                }
                load()
            }
            is FreeUpSpaceUseCase.FreeUpResult.NeedsPermission -> {
                pendingDeleteBridge.offer(result.localUris)
                _uiState.update { it.copy(phase = FreeUpSpacePhase.Ready) }
            }
            is FreeUpSpaceUseCase.FreeUpResult.Failed -> {
                _uiState.update {
                    it.copy(
                        phase = FreeUpSpacePhase.Error,
                        message = context.getString(R.string.settings_free_up_error, result.message),
                    )
                }
                if (result.freed > 0) load()
            }
        }
    }

    private suspend fun totalBytes(rows: List<SyncState>): Long {
        if (rows.isEmpty()) return 0L
        val sizeByUri = runCatching {
            localRepo.observeLocalMedia().first().associate { it.uri to it.sizeBytes }
        }.getOrDefault(emptyMap())
        return rows.sumOf { row ->
            row.sizeBytes.takeIf { it > 0L } ?: sizeByUri[row.localUri] ?: 0L
        }
    }

    private fun freedMessage(freed: Int): String =
        if (freed > 0) {
            context.resources.getQuantityString(R.plurals.settings_freed_photos, freed, freed)
        } else {
            context.getString(R.string.settings_free_up_none)
        }
}
