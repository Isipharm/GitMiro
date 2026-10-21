package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.LfsError
import com.hugodehu.gitmiro.domain.lfs.LfsObject
import org.eclipse.jgit.lfs.lib.AnyLongObjectId

interface IVerifyUploadLfsObjectGitAction {
    suspend operator fun invoke(
        lfsServerUrl: String,
        lfsObject: LfsObject,
        oid: AnyLongObjectId,
    ): Either<Unit, LfsError>
}