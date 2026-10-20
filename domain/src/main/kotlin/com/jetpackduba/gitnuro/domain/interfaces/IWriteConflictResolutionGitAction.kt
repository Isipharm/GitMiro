package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError

interface IWriteConflictResolutionGitAction {
    suspend operator fun invoke(repositoryPath: String, filePath: String, content: ByteArray): Either<Unit, GitError>
}
