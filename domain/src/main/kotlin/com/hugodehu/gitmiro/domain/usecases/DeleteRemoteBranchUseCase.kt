package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IDeleteBranchGitAction
import com.hugodehu.gitmiro.domain.interfaces.IDeleteRemoteBranchGitAction
import com.hugodehu.gitmiro.domain.models.Branch
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class DeleteRemoteBranchUseCase @Inject constructor(
    private val deleteRemoteBranchGitAction: IDeleteRemoteBranchGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(branch: Branch) {
        useCaseExecutor.executeLaunch(
            TaskType.DeleteBranch,
            dataToRefresh = arrayOf(DataToRefresh.ALL), // TODO Refresh only log?
        ) { repositoryPath ->
            deleteRemoteBranchGitAction(repositoryPath, branch)
        }
    }
}