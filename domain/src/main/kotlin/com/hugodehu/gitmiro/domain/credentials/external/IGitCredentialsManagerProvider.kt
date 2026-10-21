package com.hugodehu.gitmiro.domain.credentials.external

interface IGitCredentialsManagerProvider {
    fun loadPath(): String?
}