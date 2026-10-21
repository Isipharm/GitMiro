package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.ConflictSide

interface IResolveConflictWithSideGitAction {
    suspend operator fun invoke(repositoryPath: String, filePath: String, side: ConflictSide): Either<Unit, GitError>
}
