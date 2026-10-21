package com.hugodehu.gitmiro.data.git.workspace

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.interfaces.IStageByDirectoryGitAction
import javax.inject.Inject

class StageByDirectoryGitAction @Inject constructor(
    private val jgit: JGit,
) : IStageByDirectoryGitAction {
    override suspend operator fun invoke(repositoryPath: String, dir: String) = jgit.provide(repositoryPath) { git ->
        git
            .add()
            .addFilepattern(dir)
            .call()

        Unit
    }
}
