package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.Commit
import com.hugodehu.gitmiro.domain.models.Identity
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.revwalk.RevCommit

interface IDoCommitGitAction {
    suspend operator fun invoke(
        repositoryPath: String,
        message: String,
        amend: Boolean,
        author: Identity?,
    ): Either<Commit, GitError>
}