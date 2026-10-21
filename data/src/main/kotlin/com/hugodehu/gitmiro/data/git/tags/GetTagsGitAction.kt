package com.hugodehu.gitmiro.data.git.tags

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.data.mappers.JGitTagMapper
import com.hugodehu.gitmiro.domain.interfaces.IGetTagsGitAction
import javax.inject.Inject

class GetTagsGitAction @Inject constructor(
    private val tagMapper: JGitTagMapper,
    private val jgit: JGit,
) : IGetTagsGitAction {
    override suspend operator fun invoke(repositoryPath: String) = jgit.provide(repositoryPath) { git ->
        git
            .tagList()
            .call()
            .mapNotNull { tag ->
                val tag = if (!tag.isPeeled) {
                    git.repository.refDatabase.peel(tag)
                } else {
                    tag
                }

                tagMapper.toDomain(tag)
            }
    }
}