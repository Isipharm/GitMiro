package com.hugodehu.gitmiro.domain.services

interface IGitProviderService {
    fun cleanupExcept(repositoriesToKeep: Set<String>)
}