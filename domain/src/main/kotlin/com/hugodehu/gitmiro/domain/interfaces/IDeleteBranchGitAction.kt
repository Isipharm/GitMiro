package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.Branch

interface IDeleteBranchGitAction {
    suspend operator fun invoke(repositoryPath: String, branch: Branch): Either<Unit, GitError>
}