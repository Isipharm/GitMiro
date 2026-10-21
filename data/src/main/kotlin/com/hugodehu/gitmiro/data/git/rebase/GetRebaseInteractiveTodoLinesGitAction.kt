package com.hugodehu.gitmiro.data.git.rebase

import com.hugodehu.gitmiro.common.printDebug
import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.data.mappers.JGitRebaseTodoLineMapper
import com.hugodehu.gitmiro.domain.RebaseConstants
import com.hugodehu.gitmiro.domain.interfaces.IGetRebaseInteractiveTodoLinesGitAction
import org.eclipse.jgit.api.RebaseCommand
import javax.inject.Inject

private const val TAG = "GetRebaseInteractiveTod"

class GetRebaseInteractiveTodoLinesGitAction @Inject constructor(
    private val jgit: JGit,
    private val rebaseTodoLineMapper: JGitRebaseTodoLineMapper,
) : IGetRebaseInteractiveTodoLinesGitAction {
    override suspend operator fun invoke(repositoryPath: String) = jgit.provide(repositoryPath) { git ->
        val repository = git.repository

        val filePath = "${RebaseCommand.REBASE_MERGE}/${RebaseConstants.GIT_REBASE_TODO}"
        val lines = repository.readRebaseTodo(filePath, false)

        printDebug(TAG, "There are ${lines.count()} lines")

        lines.map { line ->
            rebaseTodoLineMapper.toDomain(line)
        }
    }
}