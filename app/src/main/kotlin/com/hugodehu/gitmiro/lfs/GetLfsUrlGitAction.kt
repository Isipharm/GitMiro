package com.hugodehu.gitmiro.lfs

import com.hugodehu.gitmiro.common.printError
import com.hugodehu.gitmiro.data.git.branches.GetCurrentBranchGitAction
import com.hugodehu.gitmiro.data.git.branches.GetTrackingBranchGitAction
import com.hugodehu.gitmiro.data.git.remotes.GetRemotesGitAction
import com.hugodehu.gitmiro.domain.errors.okOrNull
import com.hugodehu.gitmiro.domain.extensions.isHttpOrHttps
import com.hugodehu.gitmiro.domain.interfaces.IGetCurrentBranchGitAction
import com.hugodehu.gitmiro.domain.interfaces.IGetRemotesGitAction
import com.hugodehu.gitmiro.domain.interfaces.IGetTrackingBranchGitAction
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileBasedConfig
import java.io.File
import javax.inject.Inject

private const val TAG = "LfsRepository"

class GetLfsUrlGitAction @Inject constructor(
    private val getTrackingBranchGitAction: GetTrackingBranchGitAction,
    private val getCurrentBranchGitAction: GetCurrentBranchGitAction,
    private val getRemotesGitAction: GetRemotesGitAction,
) {
    suspend operator fun invoke(repository: Repository, remoteName: String?): String? {
        val git = Git(repository)
        // TODO This only gets the URL from considering happy path config
        val configFile = File(repository.workTree, ".lfsconfig")

        val config = FileBasedConfig(configFile, repository.fs)
        config.load()

        val lfsConfigUrl = config.getString("lfs", null, "url")

        return lfsConfigUrl ?: getLfsUrlFromRemote(git, remoteName)
    }

    private suspend fun getLfsUrlFromRemote(git: Git, remoteName: String?): String? {
        val repositoryPath = git.repository.directory.absolutePath

        val remotePath = if (remoteName != null) {
            remoteName
        } else {
            val currentBranch = getCurrentBranchGitAction(repositoryPath).okOrNull() // TODO Proper error handling?

            if (currentBranch == null) {
                printError(TAG, "Current branch is null and couldn't obtain tracking branch remote.")
                return null
            }

            val trackingBranch =
                getTrackingBranchGitAction(repositoryPath, currentBranch.name).okOrNull() // TODO Proper error handling?
            val currentBranchTracedRemote = trackingBranch?.remote

            if (currentBranchTracedRemote != null) {
                Constants.R_REMOTES + currentBranchTracedRemote
            } else {
                val remotes = getRemotesGitAction(repositoryPath).okOrNull().orEmpty()

                return if (remotes.count() == 1) {
                    val remote = remotes[0].fetchUri.removeSuffix("/")
                        .let {
                            if (it.endsWith(".git")) {
                                it
                            } else {
                                "$it.git"
                            }
                        }
                    "$remote/info/lfs"
                } else {
                    printError(TAG, "Remote name is null and couldn't obtain tracking branch remote.")
                    null
                }
            }
        }

        val remoteUrl = git
            .remoteList()
            .call()
            .firstOrNull { Constants.R_REMOTES + it.name == remotePath }
            ?.urIs
            ?.firstOrNull()

        return if (remoteUrl != null && remoteUrl.toString().isHttpOrHttps()) {
            "$remoteUrl/info/lfs"
        } else {
            remoteUrl?.toString()
        }
    }
}