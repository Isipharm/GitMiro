package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.Commit
import com.hugodehu.gitmiro.domain.usecases.ResetType
import org.eclipse.jgit.api.Git

interface IResetToCommitGitAction {
    suspend operator fun invoke(repositoryPath: String, commit: Commit, resetType: ResetType): Either<Unit, GitError>
}