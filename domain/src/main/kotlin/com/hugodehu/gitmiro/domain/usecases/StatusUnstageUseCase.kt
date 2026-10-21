package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IUnstageEntryGitAction
import com.hugodehu.gitmiro.domain.models.StatusEntry
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

private const val TAG = "StatusUnstageUseCase"


class StatusUnstageUseCase @Inject constructor(
    private val unstageEntryGitAction: IUnstageEntryGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(statusEntry: StatusEntry) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.UnstageFile,
            dataToRefresh = arrayOf(DataToRefresh.STATUS),
        ) { repositoryPath ->
            unstageEntryGitAction(repositoryPath, statusEntry)
        }
    }
}
