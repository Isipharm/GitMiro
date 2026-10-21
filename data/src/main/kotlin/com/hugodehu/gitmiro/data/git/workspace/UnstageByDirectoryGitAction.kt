package com.hugodehu.gitmiro.data.git.workspace

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.interfaces.IUnstageByDirectoryGitAction
import javax.inject.Inject

class UnstageByDirectoryGitAction @Inject constructor(
    private val jgit: JGit,
) : IUnstageByDirectoryGitAction {
    override suspend operator fun invoke(repositoryPath: String, dir: String): Either<Unit, GitError> =
        jgit.provide(repositoryPath) { git ->
            git
                .reset()
                .addPath(dir)
                .call()
        }
}
