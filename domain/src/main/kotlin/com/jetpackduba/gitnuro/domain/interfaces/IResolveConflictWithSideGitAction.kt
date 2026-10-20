package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.ConflictSide

interface IResolveConflictWithSideGitAction {
    suspend operator fun invoke(repositoryPath: String, filePath: String, side: ConflictSide): Either<Unit, GitError>
}
