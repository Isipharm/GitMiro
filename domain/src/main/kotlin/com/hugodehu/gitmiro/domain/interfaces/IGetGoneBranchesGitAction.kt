package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.Branch

interface IGetGoneBranchesGitAction {
    /**
     * Local branches, except the current one, whose upstream branch no longer exists on its remote ("gone" in
     * `git branch -vv`). Only as accurate as the last fetch with prune.
     */
    suspend operator fun invoke(repositoryPath: String): Either<List<Branch>, GitError>
}
