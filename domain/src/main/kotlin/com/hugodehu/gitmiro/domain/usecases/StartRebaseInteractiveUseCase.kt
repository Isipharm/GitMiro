package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IStartRebaseInteractiveGitAction
import com.hugodehu.gitmiro.domain.models.Commit
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class StartRebaseInteractiveUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val startRebaseInteractiveGitAction: IStartRebaseInteractiveGitAction,
) {
    operator fun invoke(commit: Commit) =  useCaseExecutor.executeLaunch(
        taskType = TaskType.RebaseInteractive,
        dataToRefresh = arrayOf(DataToRefresh.REPO_STATE),
    ) { repositoryPath ->
        startRebaseInteractiveGitAction(repositoryPath, commit)
    }
}