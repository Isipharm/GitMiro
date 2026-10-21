package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.errors.AppError
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.bind
import com.hugodehu.gitmiro.domain.interfaces.IRenameBranchGitAction
import com.hugodehu.gitmiro.domain.interfaces.ISetTrackingBranchGitAction
import javax.inject.Inject

class RenameBranchUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val renameBranchGitAction: IRenameBranchGitAction,
    private val setTrackingBranchGitAction: ISetTrackingBranchGitAction,
) {
    suspend operator fun invoke(oldName: String, newName: String): Either<Unit, AppError> {
        return useCaseExecutor.execute(
            dataToRefresh = arrayOf(DataToRefresh.BRANCHES, DataToRefresh.LOG),
        ) { repositoryPath ->
            val branch = renameBranchGitAction(repositoryPath, oldName, newName).bind()

            setTrackingBranchGitAction(repositoryPath, branch, null, null)
        }
    }
}