package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IUnstageHunkLineGitAction
import com.hugodehu.gitmiro.domain.models.Hunk
import com.hugodehu.gitmiro.domain.models.Line
import com.hugodehu.gitmiro.domain.models.TaskType
import org.eclipse.jgit.diff.DiffEntry
import javax.inject.Inject

class UnstageHunkLineUseCase @Inject constructor(
    private val unstageHunkLineGitAction: IUnstageHunkLineGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(diffEntry: DiffEntry, hunk: Hunk, line: Line) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.UnstageLine,
            dataToRefresh = arrayOf(DataToRefresh.STATUS),
        ) { repositoryPath ->
            unstageHunkLineGitAction(repositoryPath, diffEntry, hunk, line)
        }
    }
}
