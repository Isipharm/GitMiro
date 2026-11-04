package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.RebaseProgress

interface IGetRebaseProgressGitAction {
    /**
     * Steps of the rebase in progress, null when the repository is not rebasing or the rebase keeps no steps.
     */
    suspend operator fun invoke(repositoryPath: String): Either<RebaseProgress?, GitError>
}
