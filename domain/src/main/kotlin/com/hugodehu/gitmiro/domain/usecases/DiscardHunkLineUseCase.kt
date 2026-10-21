package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IDiscardUnstagedHunkLineGitAction
import com.hugodehu.gitmiro.domain.models.Hunk
import com.hugodehu.gitmiro.domain.models.Line
import com.hugodehu.gitmiro.domain.models.TaskType
import org.eclipse.jgit.diff.DiffEntry
import javax.inject.Inject

class DiscardHunkLineUseCase @Inject constructor(
    private val discardUnstagedHunkLineGitAction: IDiscardUnstagedHunkLineGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(diffEntry: DiffEntry, hunk: Hunk, line: Line) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.Unspecified,
            dataToRefresh = arrayOf(DataToRefresh.STATUS),
        ) { repositoryPath ->
            discardUnstagedHunkLineGitAction(repositoryPath, diffEntry, hunk, line)
        }
    }
}
