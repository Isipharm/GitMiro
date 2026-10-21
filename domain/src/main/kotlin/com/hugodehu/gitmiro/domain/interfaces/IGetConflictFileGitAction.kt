package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.ConflictFile

interface IGetConflictFileGitAction {
    /**
     * Returns null when the conflict can't be split in blocks: binary file or one side deleted the file.
     */
    suspend operator fun invoke(repositoryPath: String, filePath: String): Either<ConflictFile?, GitError>
}
