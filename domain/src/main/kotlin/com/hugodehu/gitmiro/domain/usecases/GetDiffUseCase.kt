package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.common.extensions.TAG
import com.hugodehu.gitmiro.common.printError
import com.hugodehu.gitmiro.domain.errors.okOrNull
import com.hugodehu.gitmiro.domain.interfaces.IFormatDiffGitAction
import com.hugodehu.gitmiro.domain.interfaces.IGenerateSplitHunkFromDiffResultGitAction
import com.hugodehu.gitmiro.domain.models.DiffResult
import com.hugodehu.gitmiro.domain.models.DiffTextViewType
import com.hugodehu.gitmiro.domain.models.DiffType
import com.hugodehu.gitmiro.domain.models.ViewDiffResult
import com.hugodehu.gitmiro.domain.repositories.RepositoryDataRepository
import com.hugodehu.gitmiro.domain.services.AppSettingsService
import org.eclipse.jgit.diff.DiffEntry
import javax.inject.Inject

class GetDiffUseCase @Inject constructor(
    private val formatDiffGitAction: IFormatDiffGitAction,
    private val generateSplitHunkFromDiffResultGitAction: IGenerateSplitHunkFromDiffResultGitAction,
    private val repositoryDataRepository: RepositoryDataRepository,
) {
    suspend operator fun invoke(diffType: DiffType, diffViewType: DiffTextViewType, isDisplayFullFile: Boolean): ViewDiffResult {
        val repositoryPath = repositoryDataRepository.repositoryPath ?: return ViewDiffResult.None

        return try {
            val diffFormat = formatDiffGitAction(repositoryPath, diffType, isDisplayFullFile).okOrNull()!!
            val diffEntry = diffFormat.diffEntry
            if (
                // Conflicts are always shown side by side: current on the left, incoming on the right
                (diffViewType == DiffTextViewType.Split || diffType.isConflict) &&
                diffFormat is DiffResult.Text &&
                diffEntry.changeType != DiffEntry.ChangeType.ADD &&
                diffEntry.changeType != DiffEntry.ChangeType.DELETE
            ) {
                val splitHunkList = generateSplitHunkFromDiffResultGitAction(diffFormat)
                ViewDiffResult.Loaded(
                    diffType,
                    DiffResult.TextSplit(diffEntry, splitHunkList)
                )
            } else {
                ViewDiffResult.Loaded(diffType, diffFormat)
            }

        } catch (ex: Exception) {
            printError(TAG, ex.message.orEmpty(), ex)

            ex.printStackTrace()
            ViewDiffResult.DiffNotFound(diffType)
        }
    }
}