package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IGetCommitStatsGitAction
import com.hugodehu.gitmiro.domain.models.Commit
import javax.inject.Inject

class GetCommitStatsUseCase @Inject constructor(
    private val getCommitStatsGitAction: IGetCommitStatsGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke(commit: Commit) = useCaseExecutor.execute { repositoryPath ->
        getCommitStatsGitAction(repositoryPath, commit)
    }
}
