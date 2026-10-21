package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IDeleteBranchGitAction
import com.hugodehu.gitmiro.domain.models.Branch
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class DeleteBranchUseCase @Inject constructor(
    private val deleteBranchGitAction: IDeleteBranchGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(branch: Branch) {
        useCaseExecutor.executeLaunch(
            TaskType.DeleteBranch,
            dataToRefresh = arrayOf(DataToRefresh.ALL),
        ) { repositoryPath ->
            deleteBranchGitAction(repositoryPath, branch)
        }
    }
}