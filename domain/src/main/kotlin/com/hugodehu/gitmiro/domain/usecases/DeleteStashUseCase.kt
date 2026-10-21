package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IDeleteStashGitAction
import com.hugodehu.gitmiro.domain.models.Commit
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class DeleteStashUseCase @Inject constructor(
    private val deleteStashGitAction: IDeleteStashGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(stash: Commit) = useCaseExecutor.executeLaunch(
        taskType = TaskType.Stash,
        refreshEvenIfFailed = true,
        dataToRefresh = arrayOf(DataToRefresh.STASHES, DataToRefresh.LOG),
    ) { repositoryPath ->
        deleteStashGitAction(repositoryPath, stash)
    }
}