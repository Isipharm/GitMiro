package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.AppError
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.models.StatusEntry
import org.eclipse.jgit.api.Git

interface IUnstageAllGitAction {
    suspend operator fun invoke(repositoryPath: String, entries: List<StatusEntry>?): Either<Unit, AppError>
}