package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IAbortRebaseGitAction
import com.hugodehu.gitmiro.domain.interfaces.IResetRepositoryStateGitAction
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class ResetRepositoryStateUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val resetRepositoryStateGitAction: IResetRepositoryStateGitAction,
) {
    operator fun invoke() {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.ResetRepoState,
            dataToRefresh = arrayOf(DataToRefresh.ALL),
        ) { repositoryPath ->
            resetRepositoryStateGitAction(repositoryPath)
        }
    }
}
