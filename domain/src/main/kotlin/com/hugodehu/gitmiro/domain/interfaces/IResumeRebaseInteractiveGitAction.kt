package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import org.eclipse.jgit.api.RebaseCommand

interface IResumeRebaseInteractiveGitAction {
    suspend operator fun invoke(repositoryPath: String, interactiveHandler: RebaseCommand.InteractiveHandler): Either<Unit, GitError>
}