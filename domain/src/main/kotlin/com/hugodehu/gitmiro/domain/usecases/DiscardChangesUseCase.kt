package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IDiscardChangesGitAction
import com.hugodehu.gitmiro.domain.models.DiscardScope
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class DiscardChangesUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val discardChangesGitAction: IDiscardChangesGitAction,
) {
    operator fun invoke(scope: DiscardScope) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.DiscardChanges,
            dataToRefresh = arrayOf(DataToRefresh.STATUS, DataToRefresh.LOG),
        ) { repositoryPath ->
            discardChangesGitAction(repositoryPath, scope)
        }
    }
}
