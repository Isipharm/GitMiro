package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.StatusEntry

interface IDiscardEntriesGitAction {
    suspend operator fun invoke(repositoryPath: String, statusEntries: List<StatusEntry>, staged: Boolean): Either<Unit, GitError>
}