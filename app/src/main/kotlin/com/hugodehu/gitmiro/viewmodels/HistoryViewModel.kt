package com.hugodehu.gitmiro.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import com.hugodehu.gitmiro.TabViewModel
import com.hugodehu.gitmiro.domain.TabCoroutineScope
import com.hugodehu.gitmiro.domain.errors.AppError
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.okOrNull
import com.hugodehu.gitmiro.domain.exceptions.MissingDiffEntryException
import com.hugodehu.gitmiro.domain.extensions.filePath
import com.hugodehu.gitmiro.domain.interfaces.IGenerateSplitHunkFromDiffResultGitAction
import com.hugodehu.gitmiro.domain.models.*
import com.hugodehu.gitmiro.domain.services.AppSettingsService
import com.hugodehu.gitmiro.domain.usecases.GetCommitDiffEntriesUseCase
import com.hugodehu.gitmiro.domain.usecases.GetDiffUseCase
import com.hugodehu.gitmiro.domain.usecases.GetFileCommitsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

class HistoryViewModel @Inject constructor(
    private val getCommitDiffEntriesUseCase: GetCommitDiffEntriesUseCase,
    private val generateSplitHunkFromDiffResultGitAction: IGenerateSplitHunkFromDiffResultGitAction,
    private val settings: AppSettingsService,
    private val tabScope: TabCoroutineScope,
    private val getFileCommitsUseCase: GetFileCommitsUseCase,
    private val getDiffUseCase: GetDiffUseCase,
) : TabViewModel() {
    private val _historyState = MutableStateFlow<HistoryState>(HistoryState.Loading(""))
    val historyState: StateFlow<HistoryState> = _historyState

    private val _viewDiffResult = MutableStateFlow<ViewDiffResult>(ViewDiffResult.None)
    val viewDiffResult: StateFlow<ViewDiffResult> = _viewDiffResult

    var filePath: String = ""

    val lazyListState = MutableStateFlow(
        LazyListState(
            0,
            0
        )
    )


    init {
        tabScope.launch {
            settings.diffTextViewType.collect { diffType ->
                if (filePath.isNotBlank()) {
                    updateDiffType(diffType)
                }
            }
        }
    }

    private fun updateDiffType(newDiffType: DiffTextViewType) {
        val viewDiffResult = this.viewDiffResult.value

        if (viewDiffResult is ViewDiffResult.Loaded) {
            val diffResult = viewDiffResult.diffResult

            if (diffResult is DiffResult.Text && newDiffType == DiffTextViewType.Split) { // Current is unified and new is split
                val hunksList = generateSplitHunkFromDiffResultGitAction(diffResult)
                _viewDiffResult.value = ViewDiffResult.Loaded(
                    diffType = viewDiffResult.diffType,
                    diffResult = DiffResult.TextSplit(diffResult.diffEntry, hunksList)
                )
            } else if (diffResult is DiffResult.TextSplit && newDiffType == DiffTextViewType.Unified) { // Current is split and new is unified
                val hunksList = diffResult.hunks.map { it.sourceHunk }

                _viewDiffResult.value = ViewDiffResult.Loaded(
                    diffType = viewDiffResult.diffType,
                    diffResult = DiffResult.Text(diffResult.diffEntry, hunksList)
                )
            }
        }
    }

    fun fileHistory(filePath: String) = viewModelScope.launch {
        this@HistoryViewModel.filePath = filePath
        _historyState.value = HistoryState.Loading(filePath)

        val log = getFileCommitsUseCase(filePath)

        _historyState.value = when (log) {
            is Either.Err -> HistoryState.Failed(filePath, log.error)

            is Either.Ok -> HistoryState.Loaded(filePath, log.value)
        }
    }

    fun selectCommit(commit: Commit) {
        viewModelScope.launch {
            try {
                val diffEntries = getCommitDiffEntriesUseCase(commit).okOrNull().orEmpty()
                val diffEntry = diffEntries.firstOrNull { entry ->
                    entry.filePath == this@HistoryViewModel.filePath
                }

                if (diffEntry == null) {
                    _viewDiffResult.value = ViewDiffResult.DiffNotFound(null)
                    return@launch
                }

                val diffType = DiffType.CommitDiff(diffEntry)
                _viewDiffResult.value = getDiffUseCase(diffType, settings.diffTextViewType.first(), settings.diffDisplayFullFile.first())
            } catch (ex: Exception) {
                if (ex is MissingDiffEntryException) {
                    //TODO Call refreshStatusUseCase ? tabState.refreshData(refreshType = RefreshType.UNCOMMITTED_CHANGES)
                    _viewDiffResult.value = ViewDiffResult.DiffNotFound(null)
                } else
                    ex.printStackTrace()
            }
        }
    }
}

sealed class HistoryState(val filePath: String) {
    class Loading(filePath: String) : HistoryState(filePath)
    class Loaded(filePath: String, val commits: List<Commit>) : HistoryState(filePath)
    class Failed(filePath: String, val error: AppError) : HistoryState(filePath)
}

