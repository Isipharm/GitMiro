package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IUnstageByDirectoryGitAction
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class UnstageByDirectoryUseCase @Inject constructor(
    private val unstageByDirectoryGitAction: IUnstageByDirectoryGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(dir: String) = useCaseExecutor.executeLaunch(
        taskType = TaskType.StageDir,
        dataToRefresh = arrayOf(DataToRefresh.STATUS),
    ) { repositoryPath ->
        unstageByDirectoryGitAction(repositoryPath, dir)
    }
}