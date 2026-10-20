package com.jetpackduba.gitnuro.domain.models

import java.io.ByteArrayOutputStream

/**
 * A line of a conflicting file. [bytes] is the exact content (including its line ending) so the resolved file keeps
 * the original encoding and line endings, while [text] is only used for display.
 */
class ConflictLine(val text: String, val bytes: ByteArray)

sealed interface ConflictChunk {
    /** Lines that both sides agree on */
    data class Common(val lines: List<ConflictLine>) : ConflictChunk

    data class Conflict(val index: Int, val current: List<ConflictLine>, val incoming: List<ConflictLine>) : ConflictChunk {
        /** Lines kept for this conflict. When both sides are kept, they follow the order in which they were chosen */
        fun linesFor(choice: ConflictChoice?): List<ConflictLine> {
            val currentLines = if (choice?.keepCurrent == true) current else emptyList()
            val incomingLines = if (choice?.keepIncoming == true) incoming else emptyList()

            return if (choice?.incomingFirst == true) incomingLines + currentLines else currentLines + incomingLines
        }
    }
}

/**
 * Use [withCurrent] and [withIncoming] to change it, so the side kept last goes after the other one.
 */
data class ConflictChoice(
    val keepCurrent: Boolean = false,
    val keepIncoming: Boolean = false,
    val incomingFirst: Boolean = false,
) {
    val isDecided: Boolean get() = keepCurrent || keepIncoming

    // Keeping a side that is already kept must not change the order (e.g. from the "keep all" checkbox)
    fun withCurrent(keep: Boolean) = when {
        keep == keepCurrent -> this
        keep -> copy(keepCurrent = true, incomingFirst = keepIncoming)
        else -> copy(keepCurrent = false)
    }

    fun withIncoming(keep: Boolean) = when {
        keep == keepIncoming -> this
        keep -> copy(keepIncoming = true, incomingFirst = !keepCurrent)
        else -> copy(keepIncoming = false)
    }
}

data class ConflictFile(val chunks: List<ConflictChunk>) {
    val conflicts: List<ConflictChunk.Conflict> get() = chunks.filterIsInstance<ConflictChunk.Conflict>()

    fun isFullyDecided(choices: Map<Int, ConflictChoice>) = conflicts.all { choices[it.index]?.isDecided == true }

    /**
     * Content of the resolved file. Conflicts without a choice keep no lines.
     */
    fun resolve(choices: Map<Int, ConflictChoice>): ByteArray {
        val lines = chunks.flatMap { chunk ->
            when (chunk) {
                is ConflictChunk.Common -> chunk.lines
                is ConflictChunk.Conflict -> chunk.linesFor(choices[chunk.index])
            }
        }

        return ByteArrayOutputStream().apply {
            lines.forEachIndexed { index, line ->
                write(line.bytes)

                // Only the last line of a side can lack a line ending (end of file), add one if more lines follow
                if (index < lines.lastIndex && line.bytes.lastOrNull() != '\n'.code.toByte()) {
                    write('\n'.code)
                }
            }
        }.toByteArray()
    }
}
