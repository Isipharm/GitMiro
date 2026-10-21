package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IRevertCommitGitAction
import com.hugodehu.gitmiro.domain.models.Commit
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class RevertCommitUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val revertCommitGitAction: IRevertCommitGitAction,
) {
    operator fun invoke(commit: Commit) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.RevertCommit,
            refreshEvenIfFailed = true,
            dataToRefresh = arrayOf(DataToRefresh.STATUS, DataToRefresh.LOG),
        ) { repositoryPath ->
            revertCommitGitAction(repositoryPath, commit)
        }
    }
}