package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IStageEntryGitAction
import com.hugodehu.gitmiro.domain.models.StatusEntry
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

private const val TAG = "StatusStageUseCase"

class StatusStageUseCase @Inject constructor(
    private val stageEntryGitAction: IStageEntryGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(statusEntry: StatusEntry) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.StageFile,
            dataToRefresh = arrayOf(DataToRefresh.STATUS),
        ) { repositoryPath ->
            stageEntryGitAction(repositoryPath, statusEntry)
        }
    }
}
