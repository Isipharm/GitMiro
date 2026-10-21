package com.hugodehu.gitmiro.data.git.workspace

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.errors.AppError
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.interfaces.IGetPersistedCommitMessagesGitAction
import com.hugodehu.gitmiro.domain.models.PersistedCommitMessage
import javax.inject.Inject

class GetPersistedCommitMessagesGitAction @Inject constructor(
    private val jgit: JGit,
) : IGetPersistedCommitMessagesGitAction {
    override suspend fun invoke(repositoryPath: String): Either<PersistedCommitMessage, AppError> {
        return jgit.provide(repositoryPath) { git ->
            val commitMessage = git.repository.readCommitEditMsg()
            val mergeMessage = git.repository.readMergeCommitMsg()
            val squashMessage = git.repository.readSquashCommitMsg()

            PersistedCommitMessage(commitMessage, mergeMessage, squashMessage)
        }
    }
}