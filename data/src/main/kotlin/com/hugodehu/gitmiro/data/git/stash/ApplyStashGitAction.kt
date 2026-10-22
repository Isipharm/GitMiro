package com.hugodehu.gitmiro.data.git.stash

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.interfaces.IApplyStashGitAction
import com.hugodehu.gitmiro.domain.models.Commit
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.StashApplyFailureException
import org.eclipse.jgit.dircache.Checkout
import org.eclipse.jgit.dircache.DirCacheCheckout.CheckoutMetadata
import org.eclipse.jgit.dircache.DirCacheEntry
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.TreeWalk
import java.io.File
import javax.inject.Inject

/**
 * Index of the stash commit parent that holds the untracked files (stash created with untracked files)
 */
private const val UNTRACKED_FILES_PARENT = 2

class ApplyStashGitAction @Inject constructor(
    private val jgit: JGit,
) : IApplyStashGitAction {
    override suspend operator fun invoke(repositoryPath: String, stashInfo: Commit) = jgit.provide(repositoryPath) { git ->
        invoke(git, stashInfo)
    }

    operator fun invoke(git: Git, stashInfo: Commit) {
        try {
            git.stashApply()
                .setStashRef(stashInfo.hash)
                .call()
        } catch (ex: StashApplyFailureException) {
            // JGit restores the untracked files of the stash only when applying it has no conflicts, while git restores
            // them first. Without this, new files of the stash vanish from the working tree when there are conflicts.
            if (git.status().call().conflicting.isNotEmpty()) {
                restoreUntrackedFiles(git, stashInfo.hash)
            }

            throw ex
        }
    }

    /**
     * Writes the untracked files of the stash that don't exist in the working tree. Existing files are never
     * overwritten.
     */
    private fun restoreUntrackedFiles(git: Git, stashHash: String) {
        val repository = git.repository

        val untrackedTree = RevWalk(repository).use { walk ->
            val stashCommit = walk.parseCommit(repository.resolve(stashHash))

            if (stashCommit.parentCount <= UNTRACKED_FILES_PARENT) {
                return
            }

            walk.parseCommit(stashCommit.getParent(UNTRACKED_FILES_PARENT)).tree
        }

        val checkout = Checkout(repository)

        TreeWalk(repository).use { treeWalk ->
            treeWalk.addTree(untrackedTree)
            treeWalk.isRecursive = true
            treeWalk.operationType = TreeWalk.OperationType.CHECKOUT_OP

            while (treeWalk.next()) {
                if (File(repository.workTree, treeWalk.pathString).exists()) {
                    continue
                }

                val entry = DirCacheEntry(treeWalk.rawPath).apply {
                    fileMode = treeWalk.getFileMode(0)
                    setObjectId(treeWalk.getObjectId(0))
                }

                // Same line endings and smudge filters as any other checkout
                val metadata = CheckoutMetadata(
                    treeWalk.getEolStreamType(TreeWalk.OperationType.CHECKOUT_OP),
                    treeWalk.getSmudgeCommand(0),
                )

                checkout.checkout(entry, metadata, treeWalk.objectReader, null)
            }
        }
    }
}
