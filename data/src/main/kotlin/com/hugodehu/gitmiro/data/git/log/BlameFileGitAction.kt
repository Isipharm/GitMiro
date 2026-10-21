package com.hugodehu.gitmiro.data.git.log

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.interfaces.IBlameFileGitAction
import javax.inject.Inject

class BlameFileGitAction @Inject constructor(
    private val jgit: JGit,
) : IBlameFileGitAction {
    override suspend fun invoke(
        repositoryPath: String,
        filePath: String
    ) = jgit.provide(repositoryPath) { git ->
        git.blame()
            .setFilePath(filePath)
            .setFollowFileRenames(true)
            .call() ?: throw Exception("File is no longer present in the workspace and can't be blamed")
    }
}