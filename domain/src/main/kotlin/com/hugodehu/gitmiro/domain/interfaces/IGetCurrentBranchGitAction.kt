package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.AppError
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.models.Branch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

interface IGetCurrentBranchGitAction {
    suspend operator fun invoke(git: Git): Either<Branch?, AppError>

    suspend operator fun invoke(path: String): Either<Branch?, AppError>
}