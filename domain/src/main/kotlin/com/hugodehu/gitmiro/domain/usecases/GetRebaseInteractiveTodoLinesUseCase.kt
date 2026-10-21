package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.errors.AppError
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.interfaces.IGetRebaseInteractiveTodoLinesGitAction
import com.hugodehu.gitmiro.domain.models.RebaseLine
import javax.inject.Inject

class GetRebaseInteractiveTodoLinesUseCase @Inject constructor(
    private val getRebaseInteractiveTodoLinesGitAction: IGetRebaseInteractiveTodoLinesGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke(): Either<List<RebaseLine>, AppError> {
        return useCaseExecutor.execute(
        ) { repositoryPath ->
            getRebaseInteractiveTodoLinesGitAction(repositoryPath)
        }
    }
}