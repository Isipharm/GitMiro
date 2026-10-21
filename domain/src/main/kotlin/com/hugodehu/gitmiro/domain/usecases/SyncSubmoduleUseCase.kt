package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.ISyncSubmoduleGitAction
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class SyncSubmoduleUseCase @Inject constructor(
    private val syncSubmoduleGitAction: ISyncSubmoduleGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(submodulePath: String) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.SyncSubmodule,
            dataToRefresh = arrayOf(DataToRefresh.SUBMODULES),
        ) { repository ->
            syncSubmoduleGitAction(repository, submodulePath)
        }
    }
}