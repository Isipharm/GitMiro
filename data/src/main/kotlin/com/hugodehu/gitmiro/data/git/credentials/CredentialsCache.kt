package com.hugodehu.gitmiro.data.git.credentials

import org.eclipse.jgit.transport.Transport


interface CredentialsCache {
    suspend fun cacheCredentialsIfNeeded()
}

interface CredentialsHandler {
    fun handleTransport(transport: Transport?)
}