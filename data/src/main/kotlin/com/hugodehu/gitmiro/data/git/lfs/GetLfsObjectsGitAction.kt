package com.hugodehu.gitmiro.data.git.lfs

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.LfsError
import com.hugodehu.gitmiro.domain.interfaces.IGetLfsObjectsGitAction
import com.hugodehu.gitmiro.domain.lfs.LfsObjectBatch
import com.hugodehu.gitmiro.domain.lfs.LfsObjects
import com.hugodehu.gitmiro.domain.models.OperationType
import com.hugodehu.gitmiro.domain.network.NetworkConstants
import com.hugodehu.gitmiro.domain.repositories.LfsRepository
import javax.inject.Inject

class GetLfsObjectsGitAction @Inject constructor(
    private val lfsRepository: LfsRepository,
    private val provideLfsCredentialsGitAction: ProvideLfsCredentialsGitAction,
) : IGetLfsObjectsGitAction {
    override suspend operator fun invoke(
        lfsServerUrl: String,
        operationType: OperationType,
        branch: String,
        lfsObjectBatches: List<LfsObjectBatch>,
        headers: Map<String, String>,
    ): Either<LfsObjects, LfsError> {
        return if (headers.containsKey(NetworkConstants.AUTH_HEADER)) {
            lfsRepository.getLfsObjects(
                lfsServerUrl,
                operationType = operationType,
                branch = branch,
                objects = lfsObjectBatches,
                headers = headers,
                username = null,
                password = null,
            )
        } else {
            provideLfsCredentialsGitAction(
                url = lfsServerUrl,
            ) { user, password ->
                lfsRepository.getLfsObjects(
                    lfsServerUrl,
                    operationType = operationType,
                    branch = branch,
                    objects = lfsObjectBatches,
                    headers = headers,
                    username = user,
                    password = password,
                )
            }
        }
    }
}