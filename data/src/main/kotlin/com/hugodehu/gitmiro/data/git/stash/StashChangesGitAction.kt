package com.hugodehu.gitmiro.data.git.stash

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.errors.StashChangesError
import com.hugodehu.gitmiro.domain.errors.raiseError
import com.hugodehu.gitmiro.domain.interfaces.IStashChangesGitAction
import javax.inject.Inject

class StashChangesGitAction @Inject constructor(
    private val jgit: JGit,
) : IStashChangesGitAction {
    override suspend operator fun invoke(repositoryPath: String, message: String?) = jgit.provide(repositoryPath) { git ->
        val commit = git
            .stashCreate()
            .setIncludeUntracked(true)
            .apply {
                if (message != null)
                    setWorkingDirectoryMessage(message)
            }
            .call()


        if (commit == null) {
            raiseError(StashChangesError.NoDataToStash)
        }
    }
}