package com.hugodehu.gitmiro.data.git.rebase

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.interfaces.IContinueRebaseGitAction
import org.eclipse.jgit.api.RebaseCommand
import javax.inject.Inject

class ContinueRebaseGitAction @Inject constructor(
    private val jgit: JGit,
) : IContinueRebaseGitAction {
    override suspend operator fun invoke(repositoryPath: String) = jgit.provide(repositoryPath) { git ->
        git.rebase()
            .setOperation(RebaseCommand.Operation.CONTINUE)
            .call()

        // TODO Throw error if call result is not continue?
        Unit
    }
}