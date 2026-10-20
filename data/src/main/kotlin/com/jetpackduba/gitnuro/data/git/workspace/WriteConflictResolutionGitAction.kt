package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.data.git.diff.entriesForPath
import com.jetpackduba.gitnuro.domain.errors.GenericError
import com.jetpackduba.gitnuro.domain.errors.raiseError
import com.jetpackduba.gitnuro.domain.interfaces.IWriteConflictResolutionGitAction
import org.eclipse.jgit.dircache.DirCacheEntry
import java.io.File
import javax.inject.Inject

/**
 * Writes the resolved content of a conflicting file and marks it as resolved.
 */
class WriteConflictResolutionGitAction @Inject constructor(
    private val jgit: JGit,
) : IWriteConflictResolutionGitAction {
    override suspend fun invoke(
        repositoryPath: String,
        filePath: String,
        content: ByteArray,
    ) = jgit.provide(repositoryPath) { git ->
        val isInConflict = git.repository.readDirCache().entriesForPath(filePath).any { it.stage != DirCacheEntry.STAGE_0 }

        if (!isInConflict) {
            raiseError(GenericError("\"$filePath\" is not in conflict"))
        }

        // The content comes from the index blobs, so it has the line endings of the repository
        File(git.repository.workTree, filePath).writeBytes(content)
        git.add().addFilepattern(filePath).call()

        // Checking out the staged file applies the working tree line endings (core.autocrlf, .gitattributes)
        git.checkout().addPath(filePath).call()

        Unit
    }
}
