package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.ICherryPickCommitGitAction
import com.hugodehu.gitmiro.domain.models.Commit
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class CherryPickCommitUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val cherryPickGitAction: ICherryPickCommitGitAction,
) {
    operator fun invoke(commit: Commit) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.CherryPickCommit,
            dataToRefresh = arrayOf(DataToRefresh.STATUS, DataToRefresh.LOG, DataToRefresh.BRANCHES, DataToRefresh.REPO_STATE),
        ) { repositoryPath ->
            cherryPickGitAction(repositoryPath, commit)
        }
    }
}