package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.errors.RepositoryPathNotSetError
import com.hugodehu.gitmiro.domain.interfaces.ILoadSignOffConfigGitAction
import com.hugodehu.gitmiro.domain.models.SignOffConfig
import com.hugodehu.gitmiro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class LoadSignOffConfigUseCase @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
    private val loadSignOffConfigGitAction: ILoadSignOffConfigGitAction,
) {
    suspend operator fun invoke(): Either<SignOffConfig, GitError> {
        val repositoryPath = repositoryDataRepository.repositoryPath ?: return Either.Err(RepositoryPathNotSetError)
        return loadSignOffConfigGitAction(repositoryPath)
    }
}