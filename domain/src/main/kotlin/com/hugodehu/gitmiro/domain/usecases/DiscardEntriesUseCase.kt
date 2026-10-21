package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IDiscardEntriesGitAction
import com.hugodehu.gitmiro.domain.models.StatusEntry
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class DiscardEntriesUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val discardEntriesGitAction: IDiscardEntriesGitAction,
) {
    operator fun invoke(statusEntries: List<StatusEntry>, isStaged: Boolean) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.DiscardFile,
            dataToRefresh = arrayOf(DataToRefresh.STATUS, DataToRefresh.LOG),
        ) { repositoryPath ->
            discardEntriesGitAction(repositoryPath, statusEntries, isStaged)
        }
    }
}
