package com.hugodehu.gitmiro.data.git.workspace

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.data.git.RawFileManager
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.interfaces.IStageHunkLineGitAction
import com.hugodehu.gitmiro.domain.models.EntryContent
import com.hugodehu.gitmiro.domain.models.Hunk
import com.hugodehu.gitmiro.domain.models.Line
import com.hugodehu.gitmiro.domain.models.LineType
import org.eclipse.jgit.diff.DiffEntry
import java.nio.ByteBuffer
import javax.inject.Inject

class StageHunkLineGitAction @Inject constructor(
    private val jgit: JGit,
    private val rawFileManager: RawFileManager,
    private val getLinesFromRawTextGitAction: GetLinesFromRawTextGitAction,
) : IStageHunkLineGitAction {
    override suspend operator fun invoke(
        repositoryPath: String,
        diffEntry: DiffEntry,
        hunk: Hunk,
        line: Line
    ) = jgit.provide(repositoryPath) { git ->
        val repository = git.repository
        val dirCache = repository.lockDirCache()
        val dirCacheEditor = dirCache.editor()
        var completedWithErrors = true

        try {
            val entryContent = rawFileManager.getRawContent(
                repository = git.repository,
                side = DiffEntry.Side.OLD,
                entry = diffEntry,
                oldTreeIterator = null,
                newTreeIterator = null
            )

            if (entryContent !is EntryContent.Text)
                return@provide

            val textLines = getLinesFromRawTextGitAction(entryContent.rawText).toMutableList()

            when (line.lineType) {
                LineType.ADDED -> {
                    val previousContextLine = hunk.lines
                        .takeWhile { it != line }
                        .lastOrNull { it.lineType == LineType.CONTEXT }

                    val startingIndex = previousContextLine?.oldLineNumber ?: -1

                    textLines.add(startingIndex + 1, line.text)
                }

                LineType.REMOVED -> {
                    textLines.removeAt(line.oldLineNumber)
                }

                else -> {}
            }

            val stagedFileText = textLines.joinToString("")
            dirCacheEditor.add(
                HunkEdit(
                    diffEntry.newPath,
                    repository,
                    ByteBuffer.wrap(stagedFileText.toByteArray())
                )
            )
            dirCacheEditor.commit()

            completedWithErrors = false
        } finally {
            if (completedWithErrors)
                dirCache.unlock()
        }
    }
}
