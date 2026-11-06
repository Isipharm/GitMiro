package com.hugodehu.gitmiro.data.git.rebase

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.interfaces.ISkipRebaseGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseCommand
import javax.inject.Inject

class SkipRebaseGitAction @Inject constructor(
    private val jgit: JGit,
) : ISkipRebaseGitAction {
    override suspend operator fun invoke(repositoryPath: String) = jgit.provide(repositoryPath) { git ->
        val result = git.rebase()
            .setOperation(RebaseCommand.Operation.SKIP)
            .call()

        checkRebaseStepResult(git, result)
    }
}