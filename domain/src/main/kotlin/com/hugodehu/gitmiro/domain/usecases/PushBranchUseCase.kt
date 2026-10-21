package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IPushBranchGitAction
import com.hugodehu.gitmiro.domain.models.Branch
import com.hugodehu.gitmiro.domain.models.TaskType
import com.hugodehu.gitmiro.domain.services.AppSettingsService
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class PushBranchUseCase @Inject constructor(
    private val pushBranchGitAction: IPushBranchGitAction,
    private val appSettingsService: AppSettingsService,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(force: Boolean, pushTags: Boolean, targetRemoteBranch: Branch? = null) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.Push,
            dataToRefresh = arrayOf(DataToRefresh.LOG, DataToRefresh.REMOTES, DataToRefresh.BRANCHES),
        ) { repositoryPath ->
            val pushWithLease = appSettingsService.pushWithLease.first()

            pushBranchGitAction(repositoryPath, force, pushTags, pushWithLease, targetRemoteBranch)
        }
    }
}