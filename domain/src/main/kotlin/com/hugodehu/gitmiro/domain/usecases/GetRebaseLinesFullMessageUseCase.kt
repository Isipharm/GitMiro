package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.errors.AppError
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.bind
import com.hugodehu.gitmiro.domain.interfaces.IGetCommitFromRebaseLineGitAction
import com.hugodehu.gitmiro.domain.models.RebaseLine
import javax.inject.Inject

class GetRebaseLinesFullMessageUseCase @Inject constructor(
    private val getCommitFromRebaseLineGitAction: IGetCommitFromRebaseLineGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke(lines: List<RebaseLine>): Either<List<RebaseLine>, AppError> {
        return useCaseExecutor.execute { repositoryPath ->
            val result = lines.mapNotNull { line ->
                val commit = getCommitFromRebaseLineGitAction(repositoryPath, line.commit, line.shortMessage).bind() ?: return@mapNotNull null
                val fullMessage = commit.message
                line.copy(commit = commit.hash, fullMessage = fullMessage)
            }

            Either.Ok(result)
        }
    }
}