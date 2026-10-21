package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.AppError
import com.hugodehu.gitmiro.domain.errors.Either
import org.eclipse.jgit.lib.Repository
import java.io.File

interface IOpenRepositoryGitAction {
    suspend operator fun invoke(directory: String): Either<String, AppError>
}