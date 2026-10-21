package com.hugodehu.gitmiro.data.git.signers

import com.hugodehu.gitmiro.Signing
import com.hugodehu.gitmiro.common.extensions.TAG
import com.hugodehu.gitmiro.common.printError
import com.hugodehu.gitmiro.domain.credentials.CredentialsStateManager
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.SshSigningError
import com.hugodehu.gitmiro.domain.errors.handleException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.transport.CredentialsProvider
import java.io.File
import javax.inject.Inject

class SshSigner @Inject constructor(
    private val credentialsStateManager: CredentialsStateManager,
) : Signer {
    override fun sign(
        repository: Repository?,
        config: GpgConfig?,
        data: ByteArray,
        committer: PersonIdent?,
        signingKey: String?,
        credentialsProvider: CredentialsProvider?
    ): GpgSignature {
        // TODO Do we handle null signing key differently?
        if (signingKey == null) {
            throw CancellationException("Signing key not specified")
        }

        var password = ""
        var result: Either<String, SshSigningError>
        do {
            result = signData(data, signingKey, password)

            if (result is Either.Ok) {
                return GpgSignature(result.value.toByteArray(Charsets.UTF_8))
            }

            val credentials = runBlocking { // TODO Run blocking perhaps could be replaced?
                val isRetry = password.isNotEmpty() && result is Either.Err

                credentialsStateManager.requestSshCredentials(isRetry, password)
            }

            password = credentials.password
        } while (result is Either.Err)

        throw CancellationException("Signing cancelled")
    }

    private fun signData(data: ByteArray, signingKey: String, password: String?): Either<String, SshSigningError> {
        return runBlocking {
            handleException(
                exceptionMapper = { exception ->
                    printError(TAG, exception.message.orEmpty(), exception)
                    SshSigningError.InvalidPassword(password.orEmpty())
                }
            ) {
                val signer = Signing()
                signer.signData(data, signingKey, password)
            }
        }
    }

    override fun canLocateSigningKey(
        repository: Repository?,
        config: GpgConfig?,
        committer: PersonIdent?,
        signingKey: String?,
        credentialsProvider: CredentialsProvider?
    ): Boolean {
        return true
    }

}
