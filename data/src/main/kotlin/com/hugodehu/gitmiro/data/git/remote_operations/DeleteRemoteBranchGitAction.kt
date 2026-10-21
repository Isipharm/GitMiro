package com.hugodehu.gitmiro.data.git.remote_operations

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.data.git.branches.DeleteBranchGitAction
import com.hugodehu.gitmiro.domain.errors.bind
import com.hugodehu.gitmiro.domain.interfaces.IDeleteRemoteBranchGitAction
import com.hugodehu.gitmiro.domain.models.Branch
import com.hugodehu.gitmiro.domain.models.isRejected
import com.hugodehu.gitmiro.domain.models.statusMessage
import org.eclipse.jgit.transport.RefSpec
import javax.inject.Inject

class DeleteRemoteBranchGitAction @Inject constructor(
    private val handleTransportGitAction: HandleTransportGitAction,
    private val deleteBranchGitAction: DeleteBranchGitAction,
    private val jgit: JGit,
) : IDeleteRemoteBranchGitAction {
    override suspend operator fun invoke(repositoryPath: String, ref: Branch) = jgit.provide(repositoryPath) { git ->
        val branchSplit = ref.name.split("/").toMutableList()
        val remoteName = branchSplit[2] // Remote name
        repeat(3) {
            branchSplit.removeAt(0)
        }

        val branchName = "refs/heads/${branchSplit.joinToString("/")}"

        val refSpec = RefSpec()
            .setSource(null)
            .setDestination(branchName)

        handleTransportGitAction(repositoryPath) {
            val pushResults = git
                .push()
                .setTransportConfigCallback {
                    handleTransport(it)
                }
                .setRefSpecs(refSpec)
                .setRemote(remoteName)
                .call()

            val results = pushResults.map { pushResult ->
                pushResult.remoteUpdates.filter { remoteRefUpdate ->
                    remoteRefUpdate.status.isRejected
                }
            }.flatten()

            if (results.isNotEmpty()) {
                val error = StringBuilder()

                results.forEach { result ->
                    error.append(result.statusMessage)
                    error.append("\n")
                }

                throw Exception(error.toString())
            }
        }

        deleteBranchGitAction(repositoryPath, ref).bind() /// TODO Handle error?

    }
}