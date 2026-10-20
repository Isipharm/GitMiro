package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.errors.okOrNull
import com.jetpackduba.gitnuro.domain.interfaces.IGetConflictFileGitAction
import com.jetpackduba.gitnuro.domain.models.ConflictFile
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class GetConflictFileUseCase @Inject constructor(
    private val getConflictFileGitAction: IGetConflictFileGitAction,
    private val repositoryDataRepository: RepositoryDataRepository,
) {
    suspend operator fun invoke(filePath: String): ConflictFile? {
        val repositoryPath = repositoryDataRepository.repositoryPath ?: return null

        return getConflictFileGitAction(repositoryPath, filePath).okOrNull()
    }
}
