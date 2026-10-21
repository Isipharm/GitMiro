package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.LfsError
import com.hugodehu.gitmiro.domain.lfs.LfsObject
import org.eclipse.jgit.lfs.lib.AnyLongObjectId
import org.eclipse.jgit.lib.Repository

interface IUploadLfsObjectGitAction {
    suspend operator fun invoke(
        lfsServerUrl: String,
        lfsObject: LfsObject,
        repository: Repository,
        oid: AnyLongObjectId,
    ): Either<Unit, LfsError>
}