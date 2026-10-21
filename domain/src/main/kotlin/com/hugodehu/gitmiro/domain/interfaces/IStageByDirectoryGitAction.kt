package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError

interface IStageByDirectoryGitAction {
    suspend operator fun invoke(repositoryPath: String, dir: String): Either<Unit, GitError>
}