package com.hugodehu.gitmiro.data.git.rebase

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.exceptions.UncommittedChangesDetectedException
import com.hugodehu.gitmiro.domain.interfaces.IResumeRebaseInteractiveGitAction
import org.eclipse.jgit.api.RebaseCommand
import org.eclipse.jgit.api.RebaseResult
import javax.inject.Inject

class ResumeRebaseInteractiveGitAction @Inject constructor(
    private val jgit: JGit,
) : IResumeRebaseInteractiveGitAction {
    override suspend operator fun invoke(repositoryPath: String, interactiveHandler: RebaseCommand.InteractiveHandler) =
        jgit.provide(repositoryPath) { git ->
            val rebaseResult = git.rebase()
                .runInteractively(interactiveHandler)
                .setOperation(RebaseCommand.Operation.PROCESS_STEPS)
                .call()


            when (rebaseResult.status) {
                RebaseResult.Status.FAILED -> throw UncommittedChangesDetectedException("Rebase interactive failed.")
                RebaseResult.Status.UNCOMMITTED_CHANGES, RebaseResult.Status.CONFLICTS -> throw UncommittedChangesDetectedException(
                    "You can't have uncommitted changes before starting a rebase interactive"
                )

                else -> {}
            }
        }
}