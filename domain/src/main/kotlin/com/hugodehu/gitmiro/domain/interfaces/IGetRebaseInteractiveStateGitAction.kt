package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.RebaseInteractiveState
import org.eclipse.jgit.api.Git

interface IGetRebaseInteractiveStateGitAction {
    suspend operator fun invoke(repositoryPath: String): Either<RebaseInteractiveState, GitError>
}