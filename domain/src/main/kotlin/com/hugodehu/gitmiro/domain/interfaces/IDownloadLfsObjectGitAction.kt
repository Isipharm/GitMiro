package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.lfs.LfsObject
import org.eclipse.jgit.lfs.lib.AnyLongObjectId
import org.eclipse.jgit.lib.Repository

interface IDownloadLfsObjectGitAction {
    suspend operator fun invoke(
        repository: Repository,
        lfsServerUrl: String,
        lfsObject: LfsObject,
        oid: AnyLongObjectId,
    )
}