package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.errors.bind
import com.hugodehu.gitmiro.domain.interfaces.IInitializeSubmoduleGitAction
import com.hugodehu.gitmiro.domain.interfaces.IUpdateSubmoduleGitAction
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class InitializeSubmoduleUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val initializeSubmoduleGitAction: IInitializeSubmoduleGitAction,
    private val updateSubmoduleGitAction: IUpdateSubmoduleGitAction,
) {
    operator fun invoke(path: String) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.InitSubmodule,
            dataToRefresh = arrayOf(DataToRefresh.SUBMODULES),
        ) { repositoryPath ->
            initializeSubmoduleGitAction(repositoryPath, path).bind()
            updateSubmoduleGitAction(repositoryPath, path)
        }
    }
}