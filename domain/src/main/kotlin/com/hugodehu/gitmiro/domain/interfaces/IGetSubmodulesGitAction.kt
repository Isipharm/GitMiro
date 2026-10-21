package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.Submodule

interface IGetSubmodulesGitAction {
    suspend operator fun invoke(repositoryPath: String): Either<Map<String, Submodule>, GitError>
}