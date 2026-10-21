package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.Branch

interface IPushToSpecificBranchGitAction {
    suspend operator fun invoke(repositoryPath: String, force: Boolean, pushTags: Boolean, remoteBranch: Branch): Either<Unit, GitError>
}