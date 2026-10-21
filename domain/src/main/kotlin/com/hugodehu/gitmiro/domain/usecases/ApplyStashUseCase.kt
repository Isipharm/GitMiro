package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IApplyStashGitAction
import com.hugodehu.gitmiro.domain.models.Commit
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class ApplyStashUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val applyStashGitAction: IApplyStashGitAction,
) {
    operator fun invoke(stashCommit: Commit) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.ApplyStash,
            dataToRefresh = arrayOf(DataToRefresh.ALL),
            refreshEvenIfFailed = true,
        ) { repositoryPath ->
            applyStashGitAction(repositoryPath, stashCommit)
        }
    }
}