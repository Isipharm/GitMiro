package com.hugodehu.gitmiro.data.git.workspace

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.interfaces.IUnstageAllGitAction
import com.hugodehu.gitmiro.domain.models.StatusEntry
import javax.inject.Inject

class UnstageAllGitAction @Inject constructor(
    private val jgit: JGit,
) : IUnstageAllGitAction {
    override suspend operator fun invoke(repositoryPath: String, entries: List<StatusEntry>?) =
        jgit.provide(repositoryPath) { git ->
            git
                .reset()
                .apply {
                    entries?.forEach { entry ->
                        addPath(entry.filePath)
                    }
                }
                .call()

            Unit
        }
}