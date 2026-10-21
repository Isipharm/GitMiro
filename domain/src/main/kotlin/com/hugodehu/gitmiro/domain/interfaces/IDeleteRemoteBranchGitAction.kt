package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.Branch

interface IDeleteRemoteBranchGitAction {
    suspend operator fun invoke(repositoryPath: String, ref: Branch): Either<Unit, GitError>
}