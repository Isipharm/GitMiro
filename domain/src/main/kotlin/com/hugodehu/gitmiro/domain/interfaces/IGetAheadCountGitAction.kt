package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError

interface IGetAheadCountGitAction {
    /**
     * Commits of the current branch not yet on its upstream, 0 when there's no upstream
     */
    suspend operator fun invoke(repositoryPath: String): Either<Int, GitError>
}
