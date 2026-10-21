package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.Hunk
import com.hugodehu.gitmiro.domain.models.Line
import org.eclipse.jgit.diff.DiffEntry

interface IDiscardUnstagedHunkLineGitAction {
    suspend operator fun invoke(repositoryPath: String, diffEntry: DiffEntry, hunk: Hunk, line: Line): Either<Unit, GitError>
}