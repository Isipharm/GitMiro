package com.hugodehu.gitmiro.data.git.workspace

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.interfaces.IStageEntryGitAction
import com.hugodehu.gitmiro.domain.models.StatusEntry
import com.hugodehu.gitmiro.domain.models.StatusType
import javax.inject.Inject

class StageEntryGitAction @Inject constructor(
    private val jgit: JGit,
) : IStageEntryGitAction {
    override suspend operator fun invoke(repositoryPath: String, statusEntry: StatusEntry) =
        jgit.provide(repositoryPath) { git ->
            git
                .add()
                .addFilepattern(statusEntry.filePath)
                .setUpdate(statusEntry.statusType == StatusType.REMOVED)
                .call()

            Unit
        }
}