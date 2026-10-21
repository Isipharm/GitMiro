package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.models.Hunk
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.patch.FileHeader

interface IFormatHunksGitAction {
    operator fun invoke(
        fileHeader: FileHeader,
        rawOld: RawText,
        rawNew: RawText,
        isDisplayFullFile: Boolean,
    ): List<Hunk>
}