package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IGetRebaseProgressGitAction
import javax.inject.Inject

class GetRebaseProgressUseCase @Inject constructor(
    private val getRebaseProgressGitAction: IGetRebaseProgressGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke() = useCaseExecutor.execute { repositoryPath ->
        getRebaseProgressGitAction(repositoryPath)
    }
}
