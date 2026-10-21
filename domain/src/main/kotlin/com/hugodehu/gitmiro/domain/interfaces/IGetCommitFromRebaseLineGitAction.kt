package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.Commit

interface IGetCommitFromRebaseLineGitAction {
    suspend operator fun invoke(repositoryPath: String, commitHash: String, shortMessage: String): Either<Commit?, GitError>
}