package com.hugodehu.gitmiro.data.git.submodules

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.interfaces.IInitializeSubmoduleGitAction
import javax.inject.Inject

class InitializeSubmoduleGitAction @Inject constructor(
    private val jgit: JGit,
) : IInitializeSubmoduleGitAction {
    override suspend operator fun invoke(repositoryPath: String, path: String) = jgit.provide(repositoryPath) { git ->
        git.submoduleInit()
            .addPath(path)
            .call()

        Unit
    }
}