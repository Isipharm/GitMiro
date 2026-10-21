package com.hugodehu.gitmiro.data.git.lfs

import com.hugodehu.gitmiro.domain.interfaces.IDownloadLfsObjectGitAction
import com.hugodehu.gitmiro.domain.lfs.LfsObject
import com.hugodehu.gitmiro.domain.network.NetworkConstants
import com.hugodehu.gitmiro.domain.repositories.LfsRepository
import org.eclipse.jgit.lfs.Lfs
import org.eclipse.jgit.lfs.lib.AnyLongObjectId
import org.eclipse.jgit.lib.Repository
import javax.inject.Inject

class DownloadLfsObjectGitAction @Inject constructor(
    private val lfsRepository: LfsRepository,
    private val provideLfsCredentialsGitAction: ProvideLfsCredentialsGitAction,
) : IDownloadLfsObjectGitAction {
    override suspend operator fun invoke(
        repository: Repository,
        lfsServerUrl: String,
        lfsObject: LfsObject,
        oid: AnyLongObjectId,
    ) {
        val lfs = Lfs(repository)
        val downloadUrl = lfsObject.actions?.download?.href ?: return
        val headers = lfsObject.actions?.download?.header.orEmpty()

        if (headers.containsKey(NetworkConstants.AUTH_HEADER)) {
            lfsRepository.downloadObject(
                downloadUrl = downloadUrl,
                outPath = lfs.getMediaFile(oid),
                headers = headers,
                username = null,
                password = null
            )
        } else {
            provideLfsCredentialsGitAction(
                lfsServerUrl
            ) { user, password ->
                lfsRepository.downloadObject(
                    downloadUrl = downloadUrl,
                    outPath = lfs.getMediaFile(oid),
                    headers = headers,
                    username = user,
                    password = password,
                )
            }
        }
    }
}