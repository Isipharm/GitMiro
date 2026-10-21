package com.hugodehu.gitmiro.data.git.lfs

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.LfsError
import com.hugodehu.gitmiro.domain.interfaces.IVerifyUploadLfsObjectGitAction
import com.hugodehu.gitmiro.domain.lfs.LfsObject
import com.hugodehu.gitmiro.domain.network.NetworkConstants
import com.hugodehu.gitmiro.domain.repositories.LfsRepository
import org.eclipse.jgit.lfs.lib.AnyLongObjectId
import javax.inject.Inject


class VerifyUploadLfsObjectGitAction @Inject constructor(
    private val lfsRepository: LfsRepository,
    private val provideLfsCredentialsGitAction: ProvideLfsCredentialsGitAction,
) : IVerifyUploadLfsObjectGitAction {
    override suspend operator fun invoke(
        lfsServerUrl: String,
        lfsObject: LfsObject,
        oid: AnyLongObjectId,
    ): Either<Unit, LfsError> {
        val verifyUrl = lfsObject.actions?.verify?.href

        if (verifyUrl != null) {
            val verifyHeaders = lfsObject.actions?.verify?.header.orEmpty()
            return if (verifyHeaders.containsKey(NetworkConstants.AUTH_HEADER)) {
                lfsRepository.verify(
                    verifyUrl,
                    oid.name(),
                    lfsObject.size,
                    verifyHeaders,
                    null,
                    null,
                )
            } else {
                provideLfsCredentialsGitAction(
                    url = lfsServerUrl,
                ) { user, password ->
                    lfsRepository.verify(
                        verifyUrl,
                        oid.name(),
                        lfsObject.size,
                        verifyHeaders,
                        user,
                        password,
                    )
                }
            }
        }

        return Either.Ok(Unit)
    }
}