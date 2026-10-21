package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.ICreateBranchGitAction
import com.hugodehu.gitmiro.domain.models.Commit
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class CreateBranchUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    val createBranchGitAction: ICreateBranchGitAction,
) {
    operator fun invoke(branchName: String, target: Commit?) {
        // TODO Should be "execute" and handle the result in the UI
        useCaseExecutor.executeLaunch(
            taskType = TaskType.CreateBranch,
            refreshEvenIfFailed = true, // TODO Previously this was a conditional lambda: refreshEvenIfCrashesInteractive = { it is CheckoutConflictException },
            dataToRefresh = arrayOf(DataToRefresh.ALL),
        ) { repositoryPath ->
            createBranchGitAction(repositoryPath, branchName, target)
        }
    }
}