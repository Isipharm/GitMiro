package com.hugodehu.gitmiro.data.git.repository

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.interfaces.IResetRepositoryStateGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import javax.inject.Inject

class ResetRepositoryStateGitAction @Inject constructor(
    private val jgit: JGit,
) : IResetRepositoryStateGitAction {
    override suspend operator fun invoke(repositoryPath: String) = jgit.provide(repositoryPath) { git ->
        git.repository.apply {
            writeMergeCommitMsg(null)
            writeMergeHeads(null)
        }

        git.reset()
            .setMode(ResetCommand.ResetType.HARD)
            .call()

        Unit
    }
}