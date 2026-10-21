package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.models.DiffResult
import com.hugodehu.gitmiro.domain.models.SplitHunk

interface IGenerateSplitHunkFromDiffResultGitAction {
    operator fun invoke(diffFormat: DiffResult.Text): List<SplitHunk>
}