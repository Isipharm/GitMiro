package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.DiscardScope

interface IDiscardChangesGitAction {
    suspend operator fun invoke(repositoryPath: String, scope: DiscardScope): Either<Unit, GitError>
}
