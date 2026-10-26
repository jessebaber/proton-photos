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
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
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
        val rechecked: Int = 0,
        val recheckTotal: Int = 0,
        val processed: Int = 0,
        val removed: Int = 0,
        val deletionTotal: Int = 0,
        val readyCount: Int = 0,
        val message: String? = null,
        val reclaimableBytes: Long = 0L,
    ) {
        val isEmpty: Boolean get() = !loading && candidates.isEmpty() && readyCount == 0
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var finishStartedForSuccess = false
    private var finishJob: Job? = null
    private var stoppedLatch = false

    init {
        load()
        observeWorker()
        observeConsentSettlement()
    }

    fun load() {
        viewModelScope.launch {
            refreshFromStore(autoFinish = true)
        }
    }

    /**
     * Refresh candidates / ready count from the store. While Rechecking or Freeing, preserves the
     * live grid (candidates + reclaimableBytes) and only clears loading / updates readyCount.
     */
    private suspend fun refreshFromStore(autoFinish: Boolean) {
        val userId = accountManager.getPrimaryUserId().first() ?: run {
            _uiState.update { it.copy(loading = false, candidates = emptyList()) }
            return
        }
        _uiState.update { it.copy(loading = true) }
        val rows = freeUpSpace.candidates(userId, Long.MAX_VALUE, protectDownloaded = false)
        val ready = freeUpSpace.readyToReclaim(userId)
        val bytes = totalBytes(rows)
        _uiState.update {
            val liveGrid = it.phase == FreeUpSpacePhase.Rechecking ||
                it.phase == FreeUpSpacePhase.Freeing
            it.copy(
                loading = false,
                candidates = if (liveGrid) it.candidates else rows,
                readyCount = ready.size,
                reclaimableBytes = if (liveGrid) it.reclaimableBytes else bytes,
            )
        }
        // Scheduled verify parks rows in free_up_verified; start the permanent-delete finisher
        // when the Free-up screen opens with a non-empty ready set.
        val phase = _uiState.value.phase
        if (
            autoFinish &&
            ready.isNotEmpty() &&
            !finishStartedForSuccess &&
            !stoppedLatch &&
            phase != FreeUpSpacePhase.Checking &&
            phase != FreeUpSpacePhase.Rechecking &&
            phase != FreeUpSpacePhase.Freeing
        ) {
            finishStartedForSuccess = true
            finishReclaim()
        }
    }

    /** Enqueue background verify for the listed candidates. */
    fun start() {
        if (_uiState.value.phase in setOf(
                FreeUpSpacePhase.Checking,
                FreeUpSpacePhase.Rechecking,
                FreeUpSpacePhase.Freeing,
            )
        ) return
        stoppedLatch = false
        finishStartedForSuccess = false
        _uiState.update {
            it.copy(
                phase = FreeUpSpacePhase.Checking,
                checked = 0,
                checkTotal = 0,
                message = null,
            )
        }
        ManualFreeUpWorker.enqueue(context)
    }

    /** Continue a stopped operation without creating a second foreground finisher. */
    fun continueWork() {
        if (_uiState.value.phase != FreeUpSpacePhase.Stopped) return
        stoppedLatch = false
        finishStartedForSuccess = false
        if (_uiState.value.readyCount > 0) {
            finishReclaim()
        } else {
            start()
        }
    }

    /**
     * Stop either WorkManager verification or the foreground finisher. Foreground cancellation is
     * cooperative: the use case completes the current item and leaves the remaining verified rows
     * recoverable for Continue.
     */
    fun stop() {
        when (_uiState.value.phase) {
            FreeUpSpacePhase.Checking -> {
                stoppedLatch = true
                ManualFreeUpWorker.cancel(context)
                _uiState.update { it.copy(phase = FreeUpSpacePhase.Stopped) }
            }
            FreeUpSpacePhase.Rechecking,
            FreeUpSpacePhase.Freeing,
            -> {
                stoppedLatch = true
                _uiState.update { it.copy(phase = FreeUpSpacePhase.Stopped) }
                finishJob?.cancel()
            }
            else -> Unit
        }
        if (stoppedLatch) load()
    }

    /** Foreground finish: live Drive re-check then permanent delete. */
    fun finishReclaim() {
        if (_uiState.value.phase in setOf(
                FreeUpSpacePhase.Rechecking,
                FreeUpSpacePhase.Freeing,
            )
        ) return
        finishStartedForSuccess = true
        _uiState.update {
            it.copy(
                phase = FreeUpSpacePhase.Rechecking,
                rechecked = 0,
                recheckTotal = 0,
                processed = 0,
                removed = 0,
                deletionTotal = 0,
                message = null,
            )
        }
        val job = viewModelScope.launch {
            val userId = accountManager.getPrimaryUserId().first() ?: return@launch
            try {
                // Direct deletes free what this app owns; NeedsPermission URIs go to the
                // permanent-delete handler (never trash / PENDING_DELETE_URIS). All-files
                // access is optional and not required for free-up.
                val result = freeUpSpace.finishReadyForReclaim(
                    userId = userId,
                    onRecheckStarted = { ready ->
                        _uiState.update { current ->
                            val known = current.candidates.associate { it.localUri to it.sizeBytes }
                            val (settled, settleBytes) = settleFreeUpCandidates(ready, known)
                            current.copy(candidates = settled, reclaimableBytes = settleBytes)
                        }
                    },
                    onRecheckProgress = { done, total, newlyDropped ->
                        _uiState.update {
                            val stopped = it.phase == FreeUpSpacePhase.Stopped
                            var next = it.copy(rechecked = done, recheckTotal = total)
                            if (!stopped) {
                                val (pruned, pruneBytes) = pruneFreeUpCandidates(
                                    next.candidates,
                                    next.reclaimableBytes,
                                    newlyDropped,
                                )
                                next = next.copy(
                                    candidates = pruned,
                                    reclaimableBytes = pruneBytes,
                                )
                            }
                            next
                        }
                    },
                    onReclaimProgress = { processed, removed, total, newlyCompleted ->
                        // Prune in the same update that sets Freeing (unless already Stopped).
                        // Do not call load()/totalBytes() mid-run.
                        _uiState.update {
                            val stopped = it.phase == FreeUpSpacePhase.Stopped
                            var next = it.copy(
                                phase = if (stopped) {
                                    FreeUpSpacePhase.Stopped
                                } else {
                                    FreeUpSpacePhase.Freeing
                                },
                                processed = processed,
                                removed = removed,
                                deletionTotal = total,
                            )
                            if (!stopped) {
                                val (pruned, pruneBytes) = pruneFreeUpCandidates(
                                    next.candidates,
                                    next.reclaimableBytes,
                                    newlyCompleted,
                                )
                                next = next.copy(
                                    candidates = pruned,
                                    reclaimableBytes = pruneBytes,
                                )
                            }
                            next
                        }
                    },
                )
                handleReclaimResult(userId, result)
            } catch (_: CancellationException) {
                // stop() publishes Stopped before cancelling this job. Nothing is cleared here:
                // completed local deletes remain completed and unprocessed verified rows remain
                // eligible for the next Continue.
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        phase = FreeUpSpacePhase.Error,
                        message = context.getString(R.string.settings_free_up_error, e.message ?: ""),
                    )
                }
            }
        }
        finishJob = job
        job.invokeOnCompletion {
            if (finishJob === job) finishJob = null
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
                    val output = info?.outputData
                    val checked = if (
                        progress?.keyValueMap?.containsKey(ManualFreeUpWorker.KEY_DONE) == true
                    ) {
                        progress.getInt(ManualFreeUpWorker.KEY_DONE, 0)
                    } else {
                        output?.getInt(ManualFreeUpWorker.KEY_DONE, 0) ?: 0
                    }
                    val total = if (
                        progress?.keyValueMap?.containsKey(ManualFreeUpWorker.KEY_TOTAL) == true
                    ) {
                        progress.getInt(ManualFreeUpWorker.KEY_TOTAL, 0)
                    } else {
                        output?.getInt(ManualFreeUpWorker.KEY_TOTAL, 0) ?: 0
                    }

                    val userId = accountManager.getPrimaryUserId().first()
                    val readyCount = if (userId != null) {
                        freeUpSpace.readyToReclaimCount(userId)
                    } else {
                        0
                    }
                    val current = _uiState.value
                    val foreground = current.phase == FreeUpSpacePhase.Rechecking ||
                        current.phase == FreeUpSpacePhase.Freeing ||
                        current.phase == FreeUpSpacePhase.Stopped
                    val doneMessage = current.message != null && current.phase == FreeUpSpacePhase.Done
                    val phase = deriveFreeUpSpaceState(
                        workRunning = running,
                        workSucceeded = succeeded,
                        workFailed = failed,
                        readyCount = readyCount,
                        freeing = current.phase == FreeUpSpacePhase.Freeing,
                        doneMessage = doneMessage,
                        rechecking = current.phase == FreeUpSpacePhase.Rechecking,
                        stopped = stoppedLatch,
                    )
                    _uiState.update {
                        it.copy(
                            phase = if (foreground) current.phase else phase,
                            checked = if (foreground) it.checked else checked,
                            checkTotal = if (foreground) it.checkTotal else total,
                            readyCount = readyCount,
                        )
                    }
                    // Auto-start reclaim when a ready set exists (manual SUCCEEDED or scheduled
                    // verify) and the screen is open — only after the initial load has finished.
                    if (readyCount > 0 &&
                        !finishStartedForSuccess &&
                        !stoppedLatch &&
                        !foreground &&
                        !running &&
                        !current.loading
                    ) {
                        finishStartedForSuccess = true
                        finishReclaim()
                    }
                }
        }
    }

    /**
     * Reconcile typed MediaStore consent outcomes when not mid Checking/Rechecking/Freeing.
     * Grant: prune settled URIs, refresh, Done/Idle or Ready with finish latch cleared.
     * Deny: Stopped (or Idle), clear finish latch, refresh; do not auto-relaunch consent.
     */
    private fun observeConsentSettlement() {
        viewModelScope.launch {
            pendingDeleteBridge.settled.collect { settlement ->
                val phase = _uiState.value.phase
                if (phase == FreeUpSpacePhase.Checking ||
                    phase == FreeUpSpacePhase.Rechecking ||
                    phase == FreeUpSpacePhase.Freeing
                ) {
                    return@collect
                }
                if (settlement.granted) {
                    _uiState.update {
                        val (pruned, pruneBytes) = pruneFreeUpCandidates(
                            it.candidates,
                            it.reclaimableBytes,
                            settlement.uris.toList(),
                        )
                        it.copy(candidates = pruned, reclaimableBytes = pruneBytes)
                    }
                    // Keep finishStartedForSuccess true through refresh so load does not auto-finish
                    // before we decide Done vs Ready.
                    refreshFromStore(autoFinish = false)
                    finishStartedForSuccess = false
                    val readyCount = _uiState.value.readyCount
                    if (readyCount == 0) {
                        val freed = settlement.uris.size
                        _uiState.update {
                            it.copy(
                                phase = if (freed > 0) {
                                    FreeUpSpacePhase.Done
                                } else {
                                    FreeUpSpacePhase.Idle
                                },
                                message = if (freed > 0) freedMessage(freed) else null,
                            )
                        }
                    } else {
                        _uiState.update { it.copy(phase = FreeUpSpacePhase.Ready) }
                    }
                } else {
                    finishStartedForSuccess = false
                    val userId = accountManager.getPrimaryUserId().first()
                    val readyCount = if (userId != null) {
                        freeUpSpace.readyToReclaimCount(userId)
                    } else {
                        0
                    }
                    if (readyCount > 0) {
                        stoppedLatch = true
                        _uiState.update { it.copy(phase = FreeUpSpacePhase.Stopped) }
                    } else {
                        stoppedLatch = false
                        _uiState.update { it.copy(phase = FreeUpSpacePhase.Idle) }
                    }
                    refreshFromStore(autoFinish = false)
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
                if (result.completedUris.isNotEmpty()) {
                    freeUpSpace.clearVerified(userId, result.completedUris)
                } else if (result.remainingUris.isEmpty()) {
                    freeUpSpace.clearVerified(userId)
                }
                _uiState.update {
                    it.copy(
                        phase = FreeUpSpacePhase.Done,
                        message = freedMessage(result.freed),
                        readyCount = 0,
                    )
                }
                load()
            }
            is FreeUpSpaceUseCase.FreeUpResult.NeedsPermission -> {
                if (result.completedUris.isNotEmpty()) {
                    freeUpSpace.clearVerified(userId, result.completedUris)
                }
                pendingDeleteBridge.offer(result.localUris)
                _uiState.update { it.copy(phase = FreeUpSpacePhase.Ready) }
            }
            is FreeUpSpaceUseCase.FreeUpResult.Failed -> {
                if (result.completedUris.isNotEmpty()) {
                    freeUpSpace.clearVerified(userId, result.completedUris)
                }
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
