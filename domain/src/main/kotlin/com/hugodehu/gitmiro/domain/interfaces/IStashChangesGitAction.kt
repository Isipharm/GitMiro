package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError

interface IStashChangesGitAction {
    suspend operator fun invoke(repositoryPath: String, message: String?): Either<Unit, GitError>
}