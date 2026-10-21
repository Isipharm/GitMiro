package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IAbortRebaseGitAction
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class AbortRebaseUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val abortRebaseGitAction: IAbortRebaseGitAction,
) {
    operator fun invoke() {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.AbortRebase,
            dataToRefresh = arrayOf(DataToRefresh.ALL),
        ) { repositoryPath ->
            abortRebaseGitAction(repositoryPath)
        }
    }
}