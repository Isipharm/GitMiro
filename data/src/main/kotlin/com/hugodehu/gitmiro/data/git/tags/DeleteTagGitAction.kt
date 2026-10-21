package com.hugodehu.gitmiro.data.git.tags

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.interfaces.IDeleteTagGitAction
import com.hugodehu.gitmiro.domain.models.Tag
import javax.inject.Inject

class DeleteTagGitAction @Inject constructor(
    private val jgit: JGit,
) : IDeleteTagGitAction {
    override suspend operator fun invoke(repositoryPath: String, tag: Tag) = jgit.provide(repositoryPath) { git ->
        git
            .tagDelete()
            .setTags(tag.name)
            .call()

        Unit
    }
}