package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.ConflictFile

interface IGetConflictFileGitAction {
    /**
     * Returns null when the conflict can't be split in blocks: binary file or one side deleted the file.
     */
    suspend operator fun invoke(repositoryPath: String, filePath: String): Either<ConflictFile?, GitError>
}
