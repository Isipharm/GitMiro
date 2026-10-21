package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IDeleteSubmoduleGitAction
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class DeleteSubmoduleUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val deleteSubmoduleGitAction: IDeleteSubmoduleGitAction,
) {
    operator fun invoke(path: String) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.DeleteSubmodule,
            dataToRefresh = arrayOf(DataToRefresh.ALL),
        ) { repositoryPath ->
            deleteSubmoduleGitAction(repositoryPath, path)
        }
    }
}