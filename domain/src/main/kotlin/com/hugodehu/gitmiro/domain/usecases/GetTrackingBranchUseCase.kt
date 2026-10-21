package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IGetTrackingBranchGitAction
import com.hugodehu.gitmiro.domain.models.Branch
import javax.inject.Inject

class GetTrackingBranchUseCase @Inject constructor(
    private val getTrackingBranchGitAction: IGetTrackingBranchGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke(branch: Branch) = useCaseExecutor.execute { repositoryPath ->
        getTrackingBranchGitAction(repositoryPath, branch)
    }
}