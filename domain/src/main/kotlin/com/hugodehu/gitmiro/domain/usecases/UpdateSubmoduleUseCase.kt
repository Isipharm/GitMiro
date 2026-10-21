package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IDeleteSubmoduleGitAction
import com.hugodehu.gitmiro.domain.interfaces.IUpdateSubmoduleGitAction
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class UpdateSubmoduleUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val updateSubmoduleGitAction: IUpdateSubmoduleGitAction,
) {
    operator fun invoke(path: String) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.UpdateSubmodule,
            dataToRefresh = arrayOf(DataToRefresh.ALL),
        ) { repositoryPath ->
            updateSubmoduleGitAction(repositoryPath, path)
        }
    }
}