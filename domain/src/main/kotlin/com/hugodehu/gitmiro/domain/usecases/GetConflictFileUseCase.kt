package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.errors.okOrNull
import com.hugodehu.gitmiro.domain.interfaces.IGetConflictFileGitAction
import com.hugodehu.gitmiro.domain.models.ConflictFile
import com.hugodehu.gitmiro.domain.repositories.RepositoryDataRepository
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
