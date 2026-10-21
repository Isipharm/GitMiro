package com.hugodehu.gitmiro.data.git.remotes

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.interfaces.IDeleteRemoteGitAction
import javax.inject.Inject

class DeleteRemoteGitAction @Inject constructor(
    private val jgit: JGit,
) : IDeleteRemoteGitAction {
    override suspend operator fun invoke(repositoryPath: String, remoteName: String): Either<Unit, GitError> {
        return jgit.provide(repositoryPath) { git ->
            git
                .remoteRemove()
                .setRemoteName(remoteName)
                .call()
        }
    }
}