package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IGetTrackingBranchGitAction
import com.hugodehu.gitmiro.domain.interfaces.ISetTrackingBranchGitAction
import com.hugodehu.gitmiro.domain.models.Branch
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class SetTrackingBranchUseCase @Inject constructor(
    private val setTrackingBranchGitAction: ISetTrackingBranchGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(branch: Branch, remoteName: String?, remoteBranch: Branch?) = useCaseExecutor.executeLaunchAsync(
        taskType = TaskType.ChangeBranchUpstream,
        dataToRefresh = arrayOf(DataToRefresh.LOG, DataToRefresh.BRANCHES),
    ) {repositoryPath ->
        setTrackingBranchGitAction(repositoryPath, branch, remoteName, remoteBranch)
    }
}