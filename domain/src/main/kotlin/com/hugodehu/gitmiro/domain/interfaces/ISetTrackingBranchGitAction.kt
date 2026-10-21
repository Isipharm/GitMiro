package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.Branch

interface ISetTrackingBranchGitAction {
    suspend operator fun invoke(
        repositoryPath: String,
        branch: Branch,
        remoteName: String?,
        remoteBranch: Branch?
    ): Either<Unit, GitError>

    suspend operator fun invoke(
        repositoryPath: String,
        refName: String,
        remoteName: String?,
        remoteBranchName: String?
    ): Either<Unit, GitError>
}