package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError

interface ISkipRebaseGitAction {
    suspend operator fun invoke(repositoryPath: String): Either<Unit, GitError>
}