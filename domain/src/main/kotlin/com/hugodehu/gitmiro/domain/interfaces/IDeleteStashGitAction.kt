package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.Commit

interface IDeleteStashGitAction {
    suspend operator fun invoke(repositoryPath: String, stashInfo: Commit): Either<Unit, GitError>
}