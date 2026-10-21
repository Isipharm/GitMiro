package com.hugodehu.gitmiro.data.git.workspace

import com.hugodehu.gitmiro.common.use
import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.data.git.repository.GetRepositoryStateGitAction
import com.hugodehu.gitmiro.data.mappers.JGitCommitMapper
import com.hugodehu.gitmiro.data.mappers.JGitIdentityMapper
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GenericError
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.errors.HookRejectionError
import com.hugodehu.gitmiro.domain.errors.bind
import com.hugodehu.gitmiro.domain.interfaces.IDoCommitGitAction
import com.hugodehu.gitmiro.domain.models.Commit
import com.hugodehu.gitmiro.domain.models.Identity
import org.eclipse.jgit.api.errors.AbortedByHookException
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import javax.inject.Inject

private const val TAG = "DoCommitGitAction"

class DoCommitGitAction @Inject constructor(
    private val getRepositoryStateGitAction: GetRepositoryStateGitAction,
    private val commitMapper: JGitCommitMapper,
    private val identityMapper: JGitIdentityMapper,
    private val jgit: JGit,
) : IDoCommitGitAction {
    override suspend operator fun invoke(
        repositoryPath: String,
        message: String,
        amend: Boolean,
        author: Identity?,
    ): Either<Commit, GitError> = jgit.provide(
        repositoryPath,
        errorHandle = { ex ->
            if (ex is AbortedByHookException) {
//                val out = output.toString(Charsets.UTF_8)
//                printLog(TAG, out)

                // TODO Do we need to read the output as it was done before the refactor?
                HookRejectionError(ex.hookStdErr)
            } else {
                GenericError(ex.message.orEmpty())
            }
        }
    ) { git ->
        val state = getRepositoryStateGitAction(repositoryPath).bind()
        val isMerging = state.isMerging
        val output = ByteArrayOutputStream()
        val printStream = PrintStream(output, true, Charsets.UTF_8)

        use(output, printStream) {
            val commit = git
                .commit()
                .setMessage(message)
                .setAllowEmpty(amend || isMerging) // Only allow empty commits when amending
                .setAmend(amend)
                .setHookErrorStream(printStream)
                .setHookOutputStream(printStream)
                .run {
                    if (author != null) {
                        setAuthor(identityMapper.toData(author))
                    } else {
                        this
                    }
                }
                .call()

            commitMapper.toDomain(commit)
        }
    }
}