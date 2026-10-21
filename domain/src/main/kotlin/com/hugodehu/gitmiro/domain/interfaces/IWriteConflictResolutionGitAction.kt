package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError

interface IWriteConflictResolutionGitAction {
    suspend operator fun invoke(repositoryPath: String, filePath: String, content: ByteArray): Either<Unit, GitError>
}
