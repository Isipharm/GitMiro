package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.interfaces.IPullBranchGitAction
import com.hugodehu.gitmiro.domain.models.*
import com.hugodehu.gitmiro.domain.services.AppSettingsService
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class PullBranchUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val pullBranchGitAction: IPullBranchGitAction,
    private val appSettingsService: AppSettingsService,
) {
    operator fun invoke(
        pullType: PullType,
        remoteBranch: Branch? = null,
        automaticStashDescription: String,
    ) = useCaseExecutor.executeLaunch(
        taskType = TaskType.Pull,
        dataToRefresh = arrayOf(DataToRefresh.ALL),
    ) { repositoryPath ->
        val autoStashOnMerge = appSettingsService.autoStashOnMerge.first()

        val pullTypeWithSettings = if (pullType == PullType.DEFAULT) {
            val isPullWithRebase = appSettingsService.pullWithRebase.first()

            if (isPullWithRebase) {
                PullType.REBASE
            } else {
                PullType.MERGE
            }
        } else {
            pullType
        }

        val result = pullBranchGitAction(repositoryPath, pullTypeWithSettings, autoStashOnMerge, remoteBranch, automaticStashDescription)

        if (result is Either.Ok) {
            if (result.value) {
                warningNotification("Pull produced conflicts, fix them to continue")
            } else {
                positiveNotification("Pull completed")
            }
        }

        result
    }
}