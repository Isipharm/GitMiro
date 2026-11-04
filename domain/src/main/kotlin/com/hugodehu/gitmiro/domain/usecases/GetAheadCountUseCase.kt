package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IGetAheadCountGitAction
import javax.inject.Inject

class GetAheadCountUseCase @Inject constructor(
    private val getAheadCountGitAction: IGetAheadCountGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke() = useCaseExecutor.execute { repositoryPath ->
        getAheadCountGitAction(repositoryPath)
    }
}
