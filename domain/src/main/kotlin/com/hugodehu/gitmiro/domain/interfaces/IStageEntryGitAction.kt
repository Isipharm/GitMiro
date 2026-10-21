package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.AppError
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.models.StatusEntry

interface IStageEntryGitAction {
    suspend operator fun invoke(repositoryPath: String, statusEntry: StatusEntry): Either<Unit, AppError>
}