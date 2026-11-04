package com.hugodehu.gitmiro.data.git.rebase

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.interfaces.IGetRebaseProgressGitAction
import com.hugodehu.gitmiro.domain.models.RebaseProgress
import com.hugodehu.gitmiro.domain.models.RebaseProgressStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.RebaseCommand
import java.io.File
import javax.inject.Inject

class GetRebaseProgressGitAction @Inject constructor(
    private val jgit: JGit,
) : IGetRebaseProgressGitAction {
    override suspend operator fun invoke(repositoryPath: String) = jgit.provide(repositoryPath) { git ->
        val repository = git.repository

        withContext(Dispatchers.IO) {
            val rebaseDir = File(repository.directory, RebaseCommand.REBASE_MERGE)

            if (!rebaseDir.isDirectory) return@withContext null

            val done = readSteps(File(rebaseDir, "done"))
            val todo = readSteps(File(rebaseDir, "git-rebase-todo"))

            if (done.isEmpty() && todo.isEmpty()) return@withContext null

            val branch = File(rebaseDir, "head-name").readTextOrNull().orEmpty().removePrefix("refs/heads/")
            val ontoHash = File(rebaseDir, "onto").readTextOrNull()
            val onto = File(rebaseDir, "onto_name").readTextOrNull()
                ?.removePrefix("refs/heads/")?.removePrefix("refs/remotes/")
                // Git doesn't write the name of the target, so look for a branch pointing to it
                ?: repository.refDatabase.getRefsByPrefix("refs/heads/")
                    .firstOrNull { it.objectId?.name == ontoHash }?.name?.removePrefix("refs/heads/")
                ?: ontoHash?.take(7).orEmpty()

            // The step the rebase stopped on is the last one that was started
            val steps = done.mapIndexed { index, (commit, message) ->
                RebaseProgressStep(
                    commit,
                    message,
                    if (index == done.lastIndex) RebaseProgressStep.Status.CURRENT else RebaseProgressStep.Status.DONE,
                )
            } + todo.map { (commit, message) -> RebaseProgressStep(commit, message, RebaseProgressStep.Status.TODO) }

            RebaseProgress(branch, onto, steps)
        }
    }

    private fun File.readTextOrNull() = if (isFile) readText().trim() else null

    /**
     * Lines look like `pick 1a2b3c4 Commit message`, comments and empty lines are skipped.
     */
    private fun readSteps(file: File): List<Pair<String, String>> {
        if (!file.isFile) return emptyList()

        return file.readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val parts = line.trim().split(" ", limit = 3)

                if (parts.size >= 2) parts[1] to parts.getOrElse(2) { "" }.removePrefix("# ") else null
            }
    }
}
