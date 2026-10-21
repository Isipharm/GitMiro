package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IAbortRebaseGitAction
import com.hugodehu.gitmiro.domain.interfaces.ISkipRebaseGitAction
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class SkipRebaseUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val skipRebaseGitAction: ISkipRebaseGitAction,
) {
    operator fun invoke() {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.SkipRebase,
            dataToRefresh = arrayOf(DataToRefresh.ALL),
        ) { repositoryPath ->
            skipRebaseGitAction(repositoryPath)
        }
    }
}