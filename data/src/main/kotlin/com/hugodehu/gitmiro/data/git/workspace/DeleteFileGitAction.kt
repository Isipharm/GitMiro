package com.hugodehu.gitmiro.data.git.workspace

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GenericError
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.errors.raiseError
import com.hugodehu.gitmiro.domain.interfaces.IDeleteFileGitAction
import com.hugodehu.gitmiro.domain.interfaces.IDiscardEntriesGitAction
import com.hugodehu.gitmiro.domain.models.StatusEntry
import com.hugodehu.gitmiro.domain.models.StatusType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import java.io.File
import javax.inject.Inject

class DeleteFileGitAction @Inject constructor(
    private val jgit: JGit,
) : IDeleteFileGitAction {
    override suspend fun invoke(
        repositoryPath: String,
        filePath: String
    ) = jgit.provide(repositoryPath) { git ->
        val fileToDelete = File(git.repository.workTree, filePath)

        if (!fileToDelete.deleteRecursively()) {
            raiseError(GenericError("Delete file recursively failed"))
        }
    }
}