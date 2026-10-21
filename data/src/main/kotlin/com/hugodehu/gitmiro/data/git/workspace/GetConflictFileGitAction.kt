package com.hugodehu.gitmiro.data.git.workspace

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.data.git.diff.entriesForPath
import com.hugodehu.gitmiro.domain.interfaces.IGetConflictFileGitAction
import com.hugodehu.gitmiro.domain.models.ConflictChunk
import com.hugodehu.gitmiro.domain.models.ConflictFile
import com.hugodehu.gitmiro.domain.models.ConflictLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.diff.DiffAlgorithm
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.dircache.DirCacheEntry
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.merge.MergeAlgorithm
import org.eclipse.jgit.merge.MergeChunk.ConflictState
import javax.inject.Inject

private const val BASE = 0
private const val CURRENT = 1
private const val INCOMING = 2

/**
 * Splits a conflicting file in blocks by merging again its 3 index stages (base, current and incoming), so the result
 * doesn't depend on the conflict markers in the working tree.
 */
class GetConflictFileGitAction @Inject constructor(
    private val jgit: JGit,
) : IGetConflictFileGitAction {
    override suspend fun invoke(repositoryPath: String, filePath: String) = jgit.provide(repositoryPath) { git ->
        withContext(Dispatchers.IO) {
            val repository = git.repository
            val entries = repository.readDirCache().entriesForPath(filePath)

            fun stageContent(stage: Int) = entries.firstOrNull { it.stage == stage }?.let { repository.blobContent(it) }

            val current = stageContent(DirCacheEntry.STAGE_2)
            val incoming = stageContent(DirCacheEntry.STAGE_3)
            // No base when both sides added the file
            val base = stageContent(DirCacheEntry.STAGE_1) ?: ByteArray(0)

            if (current == null || incoming == null || RawText.isBinary(current) || RawText.isBinary(incoming)) {
                return@withContext null
            }

            val sequences = listOf(LinesRawText(base), LinesRawText(current), LinesRawText(incoming))
            val mergeResult = MergeAlgorithm(DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.HISTOGRAM))
                .merge(RawTextComparator.DEFAULT, sequences[BASE], sequences[CURRENT], sequences[INCOMING])

            val chunks = mutableListOf<ConflictChunk>()
            var conflictCurrent: List<ConflictLine>? = null
            var conflictIncoming: List<ConflictLine>? = null
            var conflictsCount = 0

            fun closeConflict() {
                if (conflictCurrent != null || conflictIncoming != null) {
                    chunks.add(
                        ConflictChunk.Conflict(
                            index = conflictsCount++,
                            current = conflictCurrent.orEmpty(),
                            incoming = conflictIncoming.orEmpty(),
                        )
                    )
                }
                conflictCurrent = null
                conflictIncoming = null
            }

            for (chunk in mergeResult) {
                val lines = sequences[chunk.sequenceIndex].lines(chunk.begin, chunk.end)

                when (chunk.conflictState) {
                    ConflictState.NO_CONFLICT -> {
                        closeConflict()
                        chunks.add(ConflictChunk.Common(lines))
                    }

                    ConflictState.FIRST_CONFLICTING_RANGE -> {
                        closeConflict()
                        conflictCurrent = lines
                    }

                    ConflictState.NEXT_CONFLICTING_RANGE -> conflictIncoming = lines
                    ConflictState.BASE_CONFLICTING_RANGE, null -> {} // Base lines are not shown
                }
            }
            closeConflict()

            ConflictFile(chunks)
        }
    }

    private fun Repository.blobContent(entry: DirCacheEntry): ByteArray =
        open(entry.objectId, Constants.OBJ_BLOB).bytes
}

/**
 * [RawText] giving access to the exact bytes of each line, line ending included
 */
private class LinesRawText(content: ByteArray) : RawText(content) {
    fun lines(begin: Int, end: Int): List<ConflictLine> = (begin until end).map { line ->
        // "lines" starts with a sentinel, so line N starts at lines[N + 1] and ends where the next one starts
        val bytes = content.copyOfRange(this.lines.get(line + 1), this.lines.get(line + 2))
        ConflictLine(text = getString(line).removeSuffix("\r"), bytes = bytes)
    }
}
