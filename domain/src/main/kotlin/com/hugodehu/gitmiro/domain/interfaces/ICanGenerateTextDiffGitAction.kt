package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.models.EntryContent
import org.eclipse.jgit.diff.RawText

interface ICanGenerateTextDiffGitAction {
    suspend operator fun invoke(
        rawOld: EntryContent,
        rawNew: EntryContent,
        onText: suspend (oldRawText: RawText, newRawText: RawText) -> Unit,
    ): Boolean
}