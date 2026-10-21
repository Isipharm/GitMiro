package com.hugodehu.gitmiro.data.git.workspace

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.extensions.hasUntrackedChanges
import com.hugodehu.gitmiro.domain.interfaces.ICheckHasUncommittedChangesGitAction
import javax.inject.Inject

class CheckHasUncommittedChangesGitAction @Inject constructor(
    private val jgit: JGit,
) : ICheckHasUncommittedChangesGitAction {
    override suspend operator fun invoke(repositoryPath: String): Either<Boolean, GitError> = jgit.provide(repositoryPath) { git ->
        val status = git
            .status()
            .call()

        status.hasUncommittedChanges() || status.hasUntrackedChanges()
    }
}