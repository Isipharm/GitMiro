package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IStageByDirectoryGitAction
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class StageByDirectoryUseCase @Inject constructor(
    private val stageByDirectoryGitAction: IStageByDirectoryGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(dir: String) = useCaseExecutor.executeLaunch(
        taskType = TaskType.StageDir,
        dataToRefresh = arrayOf(DataToRefresh.STATUS),
    ) { repositoryPath ->
        stageByDirectoryGitAction(repositoryPath, dir)
    }
}