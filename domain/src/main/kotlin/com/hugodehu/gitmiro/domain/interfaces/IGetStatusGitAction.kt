package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.AppError
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.models.Status

interface IGetStatusGitAction {
    suspend operator fun invoke(repository: String, paths: List<String> = emptyList()): Either<Status, AppError>
}