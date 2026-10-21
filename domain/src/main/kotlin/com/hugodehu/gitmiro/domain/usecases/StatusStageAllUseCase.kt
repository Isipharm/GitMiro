package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IStageAllGitAction
import com.hugodehu.gitmiro.domain.models.StatusEntry
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

private const val TAG = "StatusStageAllUseCase"

class StatusStageAllUseCase @Inject constructor(
    private val stageAllGitAction: IStageAllGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(entries: List<StatusEntry>?) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.StageAllFiles,
            dataToRefresh = arrayOf(DataToRefresh.STATUS),
        ) { repositoryPath ->
            stageAllGitAction(repositoryPath, entries)
        }
    }
}
