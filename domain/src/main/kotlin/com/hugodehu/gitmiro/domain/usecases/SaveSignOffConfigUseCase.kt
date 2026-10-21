package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.errors.RepositoryPathNotSetError
import com.hugodehu.gitmiro.domain.interfaces.ILoadSignOffConfigGitAction
import com.hugodehu.gitmiro.domain.interfaces.ISaveLocalRepositoryConfigGitAction
import com.hugodehu.gitmiro.domain.models.SignOffConfig
import com.hugodehu.gitmiro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class SaveSignOffConfigUseCase @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
    private val saveLocalRepositoryConfigGitAction: ISaveLocalRepositoryConfigGitAction,
) {
    suspend operator fun invoke(signOffConfig: SignOffConfig): Either<Unit, GitError> {
        val repositoryPath = repositoryDataRepository.repositoryPath ?: return Either.Err(RepositoryPathNotSetError)
        return saveLocalRepositoryConfigGitAction(repositoryPath, signOffConfig)
    }
}