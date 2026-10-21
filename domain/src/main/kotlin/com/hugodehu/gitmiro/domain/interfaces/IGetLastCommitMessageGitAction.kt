package com.hugodehu.gitmiro.domain.interfaces

import org.eclipse.jgit.api.Git

interface IGetLastCommitMessageGitAction {
    suspend operator fun invoke(git: Git): String
}