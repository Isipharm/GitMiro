package com.hugodehu.gitmiro.data.git.rebase

import com.hugodehu.gitmiro.domain.errors.EitherContext
import com.hugodehu.gitmiro.domain.errors.GenericError
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.errors.raiseError
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseResult

/**
 * JGit reports most failures of a rebase step in its result instead of throwing, and leaves the rebase in progress.
 * Stopping on new conflicts is the expected outcome and is not an error.
 */
fun EitherContext<GitError>.checkRebaseStepResult(git: Git, result: RebaseResult) {
    val commit = result.currentCommit?.name?.take(7)?.let { "Commit $it" } ?: "The next commit"

    val message = when (result.status) {
        RebaseResult.Status.STOPPED -> if (git.repository.readDirCache().hasUnmergedPaths()) {
            null
        } else {
            "$commit couldn't be applied, changes that aren't staged are in its way. Stage or discard them, then continue the rebase."
        }

        RebaseResult.Status.NOTHING_TO_COMMIT -> "Nothing to commit for this step. Skip this commit to go on with the rebase."
        RebaseResult.Status.FAILED -> "$commit couldn't be applied because of ${result.failingPaths?.keys?.joinToString().orEmpty()}."
        RebaseResult.Status.UNCOMMITTED_CHANGES -> "Uncommitted changes are in the way: ${result.uncommittedChanges?.joinToString().orEmpty()}."
        RebaseResult.Status.CONFLICTS -> "Files are in the way of $commit: ${result.conflicts?.joinToString().orEmpty()}."
        else -> null
    }

    if (message != null) {
        raiseError(GenericError(message))
    }
}
