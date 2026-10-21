package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.common.extensions.TAG
import com.hugodehu.gitmiro.common.printError
import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.onErr
import com.hugodehu.gitmiro.domain.interfaces.IPersistCommitMessageGitAction
import javax.inject.Inject

class PersistCommitMessageUseCase @Inject constructor(
    private val persistCommitMessageGitAction: IPersistCommitMessageGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke(message: String?) {
        val messageToPersist = message?.ifBlank { null }
        useCaseExecutor.execute { repositoryPath ->
            val result = persistCommitMessageGitAction(repositoryPath, messageToPersist)
                .onErr {
                    printError(TAG, "Failed to persist commit message: $it")
                }

            result
        }
    }
}
