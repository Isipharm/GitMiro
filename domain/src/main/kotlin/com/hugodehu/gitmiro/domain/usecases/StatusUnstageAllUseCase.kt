package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IUnstageAllGitAction
import com.hugodehu.gitmiro.domain.models.StatusEntry
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class StatusUnstageAllUseCase @Inject constructor(
    private val unstageAllGitAction: IUnstageAllGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(statusEntries: List<StatusEntry>?) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.UnstageAllFiles,
            dataToRefresh = arrayOf(DataToRefresh.STATUS),
        ) { repositoryPath ->
            unstageAllGitAction(repositoryPath, statusEntries)
        }
    }
}
