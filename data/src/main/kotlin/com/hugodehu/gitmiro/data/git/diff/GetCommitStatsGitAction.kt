package com.hugodehu.gitmiro.data.git.diff

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.interfaces.IGetCommitStatsGitAction
import com.hugodehu.gitmiro.domain.models.Commit
import com.hugodehu.gitmiro.domain.models.CommitStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.util.io.NullOutputStream
import javax.inject.Inject

class GetCommitStatsGitAction @Inject constructor(
    private val jgit: JGit,
) : IGetCommitStatsGitAction {
    override suspend operator fun invoke(repositoryPath: String, commit: Commit) =
        jgit.provide(repositoryPath) { git ->
            val repository = git.repository

            withContext(Dispatchers.IO) {
                val revCommit = repository.parseCommit(repository.resolve(commit.hash))
                val parent = revCommit.parents.firstOrNull()?.let { repository.parseCommit(it) }
                var added = 0
                var removed = 0

                DiffFormatter(NullOutputStream.INSTANCE).use { formatter ->
                    formatter.setRepository(repository)

                    // A null parent compares the first commit with an empty tree. Binary files have no edits
                    formatter.scan(parent, revCommit).forEach { entry ->
                        formatter.toFileHeader(entry).toEditList().forEach { edit ->
                            removed += edit.endA - edit.beginA
                            added += edit.endB - edit.beginB
                        }
                    }
                }

                CommitStats(added, removed)
            }
        }
}
