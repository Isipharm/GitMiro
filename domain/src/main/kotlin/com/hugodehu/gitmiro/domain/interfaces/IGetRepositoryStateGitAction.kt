package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.RepositoryState

interface IGetRepositoryStateGitAction {
    suspend operator fun invoke(repositoryPath: String): Either<RepositoryState, GitError>
}