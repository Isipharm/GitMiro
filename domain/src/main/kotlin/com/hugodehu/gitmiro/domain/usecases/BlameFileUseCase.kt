package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.errors.RepositoryPathNotSetError
import com.hugodehu.gitmiro.domain.interfaces.IBlameFileGitAction
import com.hugodehu.gitmiro.domain.repositories.RepositoryDataRepository
import org.eclipse.jgit.blame.BlameResult
import javax.inject.Inject

class BlameFileUseCase @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
    private val blameFileGitAction: IBlameFileGitAction,
) {
    suspend operator fun invoke(filePath: String): Either<BlameResult, GitError> {
        val repositoryPath = repositoryDataRepository.repositoryPath ?: return Either.Err(RepositoryPathNotSetError)
        return blameFileGitAction(repositoryPath, filePath)
    }
}