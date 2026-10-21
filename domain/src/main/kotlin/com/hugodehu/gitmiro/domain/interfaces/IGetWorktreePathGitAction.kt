package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError

interface IGetWorktreePathGitAction {
    suspend operator fun invoke(repositoryPath: String): Either<String, GitError>
}