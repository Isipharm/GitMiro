package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.ICheckoutCommitGitAction
import com.hugodehu.gitmiro.domain.models.Commit
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class CheckoutCommitUseCase @Inject constructor(
    private val checkoutCommitGitAction: ICheckoutCommitGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(commit: Commit) {
        invoke(commit.hash)
    }

    operator fun invoke(hash: String) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.CheckoutCommit,
            dataToRefresh = arrayOf(DataToRefresh.STATUS, DataToRefresh.LOG, DataToRefresh.BRANCHES),
        ) { repositoryPath ->
            checkoutCommitGitAction(repositoryPath, hash)
        }
    }
}