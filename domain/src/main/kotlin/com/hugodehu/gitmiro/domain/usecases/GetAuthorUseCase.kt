package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.ILoadAuthorGitAction
import javax.inject.Inject

class GetAuthorUseCase @Inject constructor(
    private val loadAuthorGitAction: ILoadAuthorGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke() = useCaseExecutor.execute() { repositoryPath ->
        loadAuthorGitAction(repositoryPath)
    }
}