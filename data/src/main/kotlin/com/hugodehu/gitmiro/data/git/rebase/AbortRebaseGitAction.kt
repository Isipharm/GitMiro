package com.hugodehu.gitmiro.data.git.rebase

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.interfaces.IAbortRebaseGitAction
import org.eclipse.jgit.api.RebaseCommand
import javax.inject.Inject

class AbortRebaseGitAction @Inject constructor(
    private val jgit: JGit,
) : IAbortRebaseGitAction {
    override suspend operator fun invoke(repositoryPath: String) = jgit.provide(repositoryPath) { git ->
        git.rebase()
            .setOperation(RebaseCommand.Operation.ABORT)
            .call()

        // TODO check if result is aborted to ensure operation worked?

        Unit
    }
}