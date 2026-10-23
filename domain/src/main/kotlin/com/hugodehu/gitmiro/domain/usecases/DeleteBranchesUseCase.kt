package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.bind
import com.hugodehu.gitmiro.domain.interfaces.IDeleteBranchGitAction
import com.hugodehu.gitmiro.domain.models.Branch
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class DeleteBranchesUseCase @Inject constructor(
    private val deleteBranchGitAction: IDeleteBranchGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(branches: List<Branch>) {
        useCaseExecutor.executeLaunch(
            TaskType.DeleteBranch,
            dataToRefresh = arrayOf(DataToRefresh.ALL),
            // Some branches may be deleted before one fails
            refreshEvenIfFailed = true,
        ) { repositoryPath ->
            for (branch in branches) {
                deleteBranchGitAction(repositoryPath, branch).bind()
            }

            Either.Ok(Unit)
        }
    }
}
