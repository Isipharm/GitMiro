package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.errors.AppError
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.interfaces.IGetWorktreePathGitAction
import javax.inject.Inject

class GetWorktreeUseCase @Inject constructor(
    private val getWorktreePathGitAction: IGetWorktreePathGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke(): Either<String, AppError> {
        return useCaseExecutor.execute(
        ) { repositoryPath ->
            getWorktreePathGitAction(repositoryPath)
        }
    }
}