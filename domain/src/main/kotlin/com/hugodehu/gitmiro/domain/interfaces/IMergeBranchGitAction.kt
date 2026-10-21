package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.Branch

interface IMergeBranchGitAction {
    /**
     * @return true if success has conflicts, false if success without conflicts
     */
    suspend operator fun invoke(
        repositoryPath: String,
        branch: Branch,
        fastForward: Boolean,
    ): Either<Boolean, GitError>
}