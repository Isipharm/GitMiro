package com.hugodehu.gitmiro.data.git.workspace

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.interfaces.IUnstageEntryGitAction
import com.hugodehu.gitmiro.domain.models.StatusEntry
import javax.inject.Inject

class UnstageEntryGitAction @Inject constructor(
    private val jgit: JGit,
) : IUnstageEntryGitAction {
    override suspend operator fun invoke(repositoryPath: String, statusEntry: StatusEntry) =
        jgit.provide(repositoryPath) { git ->
            git
                .reset()
                .addPath(statusEntry.filePath)
                .call()

            Unit
        }
}