package com.hugodehu.gitmiro.data.services

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.services.IGitProviderService
import javax.inject.Inject

class GitProviderService @Inject constructor(
    private val jgit: JGit,
) : IGitProviderService {
    override fun cleanupExcept(repositoriesToKeep: Set<String>) {
        jgit.cleanupExcept(repositoriesToKeep.map { "$it/.git" }.toSet())
    }
}