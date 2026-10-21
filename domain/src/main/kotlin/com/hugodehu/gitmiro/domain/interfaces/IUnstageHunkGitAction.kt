package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.Hunk
import org.eclipse.jgit.diff.DiffEntry

interface IUnstageHunkGitAction {
    suspend operator fun invoke(repositoryPath: String, diffEntry: DiffEntry, hunk: Hunk): Either<Unit, GitError>
}