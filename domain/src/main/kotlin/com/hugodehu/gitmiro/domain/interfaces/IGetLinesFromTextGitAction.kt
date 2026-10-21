package com.hugodehu.gitmiro.domain.interfaces

interface IGetLinesFromTextGitAction {
    operator fun invoke(content: String): List<String>
}