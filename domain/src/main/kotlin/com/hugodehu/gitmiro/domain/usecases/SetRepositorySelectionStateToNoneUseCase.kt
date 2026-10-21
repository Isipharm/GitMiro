package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.models.RepositorySelectionState
import com.hugodehu.gitmiro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class SetRepositorySelectionStateToNoneUseCase @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
) {
    operator fun invoke() {
        repositoryDataRepository.setRepositorySelectionState(RepositorySelectionState.None)
    }
}