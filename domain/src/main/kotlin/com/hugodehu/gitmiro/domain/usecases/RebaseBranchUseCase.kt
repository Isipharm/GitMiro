package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.bind
import com.hugodehu.gitmiro.domain.interfaces.IRebaseBranchGitAction
import com.hugodehu.gitmiro.domain.models.Branch
import com.hugodehu.gitmiro.domain.models.TaskType
import com.hugodehu.gitmiro.domain.models.positiveNotification
import com.hugodehu.gitmiro.domain.models.warningNotification
import javax.inject.Inject

class RebaseBranchUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val rebaseBranchGitAction: IRebaseBranchGitAction,
) {
    operator fun invoke(branch: Branch) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.RebaseBranch,
            dataToRefresh = arrayOf(DataToRefresh.ALL),
        ) { repositoryPath ->
            rebaseBranchGitAction(repositoryPath, branch)
        }
    }
}