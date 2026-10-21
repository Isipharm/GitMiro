package com.hugodehu.gitmiro.data.git.workspace

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.bind
import com.hugodehu.gitmiro.domain.errors.either
import com.hugodehu.gitmiro.domain.extensions.flatListOf
import com.hugodehu.gitmiro.domain.interfaces.IGetStatusGitAction
import com.hugodehu.gitmiro.domain.models.EntryType
import com.hugodehu.gitmiro.domain.models.Status
import com.hugodehu.gitmiro.domain.models.StatusEntry
import com.hugodehu.gitmiro.domain.models.StatusType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import org.eclipse.jgit.api.Status as JGitStatus

class GetStatusGitAction @Inject constructor(
    private val jgit: JGit,
) : IGetStatusGitAction {
    override suspend operator fun invoke(repository: String, paths: List<String>) = either {
        val status = withContext(Dispatchers.IO) {
            jgit.provide(repository) { git ->
                git
                    .status()
                    .apply {
                        for (path in paths) {
                            addPath(path)
                        }
                    }
                    .call()
            }
        }.bind()

        val staged = getStaged(status)
        val unstaged = getUnstaged(status)

        Either.Ok(Status(staged, unstaged, status.ignoredNotInIndex.toList()))
    }

    private fun getUnstaged(status: JGitStatus): List<StatusEntry> {
        val untracked = status.untracked.map {
            StatusEntry(it, StatusType.ADDED, entryType = EntryType.UNSTAGED)
        }
        val modified = status.modified.map {
            StatusEntry(it, StatusType.MODIFIED, entryType = EntryType.UNSTAGED)
        }
        val missing = status.missing.map {
            StatusEntry(it, StatusType.REMOVED, entryType = EntryType.UNSTAGED)
        }
        val conflicting = status.conflicting.map {
            StatusEntry(it, StatusType.CONFLICTING, entryType = EntryType.UNSTAGED)
        }

        return flatListOf(
            untracked,
            modified,
            missing,
            conflicting,
        ).sortedBy { it.filePath }
    }

    private fun getStaged(status: JGitStatus): List<StatusEntry> {
        val added = status.added.toStatusEntries(StatusType.ADDED, EntryType.STAGED)
        val modified = status.changed.toStatusEntries(StatusType.MODIFIED, EntryType.STAGED)
        val removed = status.removed.toStatusEntries(StatusType.REMOVED, EntryType.STAGED)

        return flatListOf(
            added,
            modified,
            removed,
        ).sortedBy { it.filePath }
    }

    private fun Set<String>.toStatusEntries(statusType: StatusType, entryType: EntryType): List<StatusEntry> {
        return this
            .map {
                StatusEntry(it, statusType, entryType)
            }
    }
}