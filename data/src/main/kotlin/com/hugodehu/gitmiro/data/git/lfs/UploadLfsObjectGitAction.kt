package com.hugodehu.gitmiro.data.git.lfs

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.LfsError
import com.hugodehu.gitmiro.domain.interfaces.IUploadLfsObjectGitAction
import com.hugodehu.gitmiro.domain.lfs.LfsObject
import com.hugodehu.gitmiro.domain.network.NetworkConstants
import com.hugodehu.gitmiro.domain.repositories.LfsRepository
import org.eclipse.jgit.lfs.Lfs
import org.eclipse.jgit.lfs.lib.AnyLongObjectId
import org.eclipse.jgit.lib.Repository
import javax.inject.Inject

class UploadLfsObjectGitAction @Inject constructor(
    private val lfsRepository: LfsRepository,
    private val provideLfsCredentialsGitAction: ProvideLfsCredentialsGitAction,
) : IUploadLfsObjectGitAction {
    override suspend operator fun invoke(
        lfsServerUrl: String,
        lfsObject: LfsObject,
        repository: Repository,
        oid: AnyLongObjectId,
    ): Either<Unit, LfsError> {
        val uploadUrl = lfsObject.actions?.upload?.href ?: return Either.Ok(Unit)

        val lfs = Lfs(repository)
        val uploadHeaders = lfsObject.actions?.upload?.header.orEmpty()

        return if (uploadHeaders.containsKey(NetworkConstants.AUTH_HEADER)) {
            lfsRepository.uploadObject(
                uploadUrl,
                oid.name(),
                lfs.getMediaFile(oid),
                lfsObject.size,
                uploadHeaders,
                null,
                null,
            )
        } else {
            provideLfsCredentialsGitAction(
                url = lfsServerUrl,
            ) { user, password ->
                lfsRepository.uploadObject(
                    uploadUrl,
                    oid.name(),
                    lfs.getMediaFile(oid),
                    lfsObject.size,
                    uploadHeaders,
                    user,
                    password,
                )
            }
        }
    }
}