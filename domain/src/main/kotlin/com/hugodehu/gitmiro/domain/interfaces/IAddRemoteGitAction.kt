package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError

interface IAddRemoteGitAction {
    suspend operator fun invoke(
        repositoryPath: String,
        remoteName: String,
        fetchUri: String,
    ): Either<Unit, GitError>
}