package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.Commit

interface ICreateSnapshotStashGitAction {
    suspend operator fun invoke(
        repositoryPath: String,
        message: String,
        includeUntracked: Boolean,
    ): Either<Commit?, GitError>
}