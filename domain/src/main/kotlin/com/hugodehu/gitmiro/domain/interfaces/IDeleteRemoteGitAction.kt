package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError

interface IDeleteRemoteGitAction {
    suspend operator fun invoke(repositoryPath: String, remoteName: String): Either<Unit, GitError>
}