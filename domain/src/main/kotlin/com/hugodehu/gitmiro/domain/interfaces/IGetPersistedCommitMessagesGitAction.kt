package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.AppError
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.models.PersistedCommitMessage

interface IGetPersistedCommitMessagesGitAction {
    suspend operator fun invoke(repositoryPath: String): Either<PersistedCommitMessage, AppError>
}