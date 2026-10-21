package com.hugodehu.gitmiro.data.git.rebase

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.RebaseConstants
import com.hugodehu.gitmiro.domain.interfaces.IGetRebaseInteractiveStateGitAction
import com.hugodehu.gitmiro.domain.models.RebaseInteractiveState
import java.io.File
import javax.inject.Inject

class GetRebaseInteractiveStateGitAction @Inject constructor(
    private val getRebaseAmendCommitIdGitAction: GetRebaseAmendCommitIdGitAction,
    private val jgit: JGit,
) : IGetRebaseInteractiveStateGitAction {
    override suspend operator fun invoke(repositoryPath: String) = jgit.provide(repositoryPath) { git ->
        // TODO Delete this action
        TODO()
    }
}