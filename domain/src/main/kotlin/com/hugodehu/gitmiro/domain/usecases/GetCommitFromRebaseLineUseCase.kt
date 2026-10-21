package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.errors.AppError
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.interfaces.IGetCommitFromRebaseLineGitAction
import com.hugodehu.gitmiro.domain.models.Commit
import javax.inject.Inject

class GetCommitFromRebaseLineUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val getCommitFromRebaseGitAction: IGetCommitFromRebaseLineGitAction,
) {
    suspend operator fun invoke(commitShortHash: String, shortMessage: String): Either<Commit?, AppError> {
        return useCaseExecutor.execute { repositoryPath ->
            getCommitFromRebaseGitAction(repositoryPath, commitShortHash, shortMessage)
        }
    }
}