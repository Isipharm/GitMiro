package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.bind
import com.hugodehu.gitmiro.domain.interfaces.ICheckHasUncommittedChangesGitAction
import com.hugodehu.gitmiro.domain.interfaces.ICreateSnapshotStashGitAction
import com.hugodehu.gitmiro.domain.interfaces.IDeleteStashGitAction
import com.hugodehu.gitmiro.domain.interfaces.IMergeBranchGitAction
import com.hugodehu.gitmiro.domain.models.Branch
import com.hugodehu.gitmiro.domain.models.Commit
import com.hugodehu.gitmiro.domain.models.TaskType
import com.hugodehu.gitmiro.domain.services.AppSettingsService
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class MergeBranchUseCase @Inject constructor(
    private val mergeBranchGitAction: IMergeBranchGitAction,
    private val appSettingsService: AppSettingsService,
    private val checkHasUncommittedChangesGitAction: ICheckHasUncommittedChangesGitAction,
    private val useCaseExecutor: UseCaseExecutor,
    private val deleteStashGitAction: IDeleteStashGitAction,
    private val createSnapshotStashGitAction: ICreateSnapshotStashGitAction,
) {

    operator fun invoke(branch: Branch, automaticStashDescription: String) {
        useCaseExecutor.executeLaunch(
            TaskType.MergeBranch,
            refreshEvenIfFailed = true,
            dataToRefresh = arrayOf(DataToRefresh.ALL),
        ) { repositoryPath ->
            val mergeAutoStash = appSettingsService.autoStashOnMerge.first()
            val fastForwardMerge = appSettingsService.fastForwardMerge.first()
            var backupStash: Commit? = null

            if (mergeAutoStash) {
                val hasUncommitedChanges = checkHasUncommittedChangesGitAction(repositoryPath).bind()
                if (hasUncommitedChanges) {
                    backupStash = createSnapshotStashGitAction(
                        repositoryPath,
                        message = automaticStashDescription,
                        includeUntracked = true
                    ).bind()
                }
            }

            val result = mergeBranchGitAction(repositoryPath, branch, fastForwardMerge)

            if (result is Either.Ok) {
                val hasConflicts = result.value

                if (!hasConflicts && backupStash != null) {
                    deleteStashGitAction(repositoryPath, backupStash)
                }
            }

            result
        }
    }
}