package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.Commit
import com.hugodehu.gitmiro.domain.models.CommitStats

interface IGetCommitStatsGitAction {
    suspend operator fun invoke(repositoryPath: String, commit: Commit): Either<CommitStats, GitError>
}
