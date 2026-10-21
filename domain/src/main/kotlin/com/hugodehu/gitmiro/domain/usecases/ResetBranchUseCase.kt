package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IResetToCommitGitAction
import com.hugodehu.gitmiro.domain.models.Commit
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class ResetBranchUseCase @Inject constructor(
    private val resetToCommitGitAction: IResetToCommitGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(revCommit: Commit, resetType: ResetType) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.ResetToCommit,
            dataToRefresh = arrayOf(DataToRefresh.ALL),
        ) { repositoryPath ->
            resetToCommitGitAction(repositoryPath, revCommit, resetType = resetType)
        }
    }
}



enum class ResetType {
    SOFT,
    MIXED,
    HARD,
}