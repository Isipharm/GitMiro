package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.RebaseLine

interface IGetRebaseInteractiveTodoLinesGitAction {
    suspend operator fun invoke(repositoryPath: String): Either<List<RebaseLine>, GitError>
}