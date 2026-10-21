package com.hugodehu.gitmiro.data.git.submodules

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.data.mappers.JGitSubmoduleMapper
import com.hugodehu.gitmiro.domain.interfaces.IGetSubmodulesGitAction
import javax.inject.Inject

class GetSubmodulesGitAction @Inject constructor(
    private val jgit: JGit,
    private val submoduleMapper: JGitSubmoduleMapper,
) : IGetSubmodulesGitAction {
    override suspend operator fun invoke(repositoryPath: String) = jgit.provide(repositoryPath) { git ->
        val submodules = git
            .submoduleStatus()
            .call()

        submodules
            .mapValues { submodule ->
                submoduleMapper.toDomain(submodule.value)
            }
            .toMap()
    }
}