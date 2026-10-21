package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.DiffResult
import com.hugodehu.gitmiro.domain.models.DiffType
import org.eclipse.jgit.api.Git

interface IFormatDiffGitAction {
    suspend operator fun invoke(
        repositoryPath: String,
        diffType: DiffType,
        isDisplayFullFile: Boolean,
    ): Either<DiffResult, GitError>
}