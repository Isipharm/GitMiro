package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError

interface IAddSubmoduleGitAction {
    suspend operator fun invoke(repositoryPath: String, name: String, path: String, uri: String): Either<Unit, GitError>
}