package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.Branch
import com.hugodehu.gitmiro.domain.models.PullType
import org.eclipse.jgit.api.Git

interface IPullBranchGitAction {
    suspend operator fun invoke(
        repositoryPath: String,
        pullType: PullType,
        mergeAutoStash: Boolean,
        remoteBranch: Branch?,
        automaticStashDescription: String,
    ): Either<PullHasConflicts, GitError>
}