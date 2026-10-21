package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.models.Line

interface ITextDiffFromDiffLinesGitAction {
    operator fun invoke(lines: List<Line>): List<Line>
}