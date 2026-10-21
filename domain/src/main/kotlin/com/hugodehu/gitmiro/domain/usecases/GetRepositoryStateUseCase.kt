package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.errors.RepositoryPathNotSetError
import com.hugodehu.gitmiro.domain.interfaces.IGetRepositoryStateGitAction
import com.hugodehu.gitmiro.domain.models.RepositoryState
import com.hugodehu.gitmiro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class GetRepositoryStateUseCase @Inject constructor(
    private val gitRepositoryStateGitAction: IGetRepositoryStateGitAction,
    private val repositoryDataRepository: RepositoryDataRepository,
) {
    suspend operator fun invoke(): Either<RepositoryState, GitError> {
        val repositoryPath = repositoryDataRepository.repositoryPath ?: return Either.Err(RepositoryPathNotSetError)
        return gitRepositoryStateGitAction(repositoryPath)
    }
}