package com.hugodehu.gitmiro.domain.models

import org.eclipse.jgit.diff.HistogramDiff
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.diff.RawTextComparator
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

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
            val currentLines = if (choice?.keepCurrent == true) {
                current.filterIndexed { index, _ -> index !in choice.excludedCurrent }
            } else {
                emptyList()
            }
            val incomingLines = if (choice?.keepIncoming == true) {
                incoming.filterIndexed { index, _ -> index !in choice.excludedIncoming }
            } else {
                emptyList()
            }

            return if (choice?.incomingFirst == true) incomingLines + currentLines else currentLines + incomingLines
        }
    }
}

/**
 * Use [withCurrent] and [withIncoming] to change it, so the side kept last goes after the other one.
 *
 * A kept side can lose some of its lines, listed in [excludedCurrent] and [excludedIncoming] by position, so lines can
 * be picked one by one from both sides. Use [withCurrentLine] and [withIncomingLine] for that.
 */
data class ConflictChoice(
    val keepCurrent: Boolean = false,
    val keepIncoming: Boolean = false,
    val incomingFirst: Boolean = false,
    val excludedCurrent: Set<Int> = emptySet(),
    val excludedIncoming: Set<Int> = emptySet(),
) {
    val isDecided: Boolean get() = keepCurrent || keepIncoming

    val isCurrentPartial: Boolean get() = keepCurrent && excludedCurrent.isNotEmpty()
    val isIncomingPartial: Boolean get() = keepIncoming && excludedIncoming.isNotEmpty()

    // Keeping a side that is already kept must not change the order (e.g. from the "keep all" checkbox), but it
    // brings back the lines that were taken out of it
    fun withCurrent(keep: Boolean) = when {
        keep == keepCurrent -> if (keep) copy(excludedCurrent = emptySet()) else this
        keep -> copy(keepCurrent = true, incomingFirst = keepIncoming, excludedCurrent = emptySet())
        else -> copy(keepCurrent = false, excludedCurrent = emptySet())
    }

    fun withIncoming(keep: Boolean) = when {
        keep == keepIncoming -> if (keep) copy(excludedIncoming = emptySet()) else this
        keep -> copy(keepIncoming = true, incomingFirst = !keepCurrent, excludedIncoming = emptySet())
        else -> copy(keepIncoming = false, excludedIncoming = emptySet())
    }

    /** Whether the line at [index] of the current side ends up in the result */
    fun keepsCurrentLine(index: Int) = keepCurrent && index !in excludedCurrent

    fun keepsIncomingLine(index: Int) = keepIncoming && index !in excludedIncoming

    /**
     * Keeps or drops one line of the current side, which has [total] lines. Dropping the last kept line drops the side.
     */
    fun withCurrentLine(index: Int, keep: Boolean, total: Int): ConflictChoice {
        // The first line picked from a side that wasn't kept starts from no lines at all
        val base = if (keepCurrent) this else withCurrent(true).copy(excludedCurrent = (0 until total).toSet())
        val excluded = if (keep) base.excludedCurrent - index else base.excludedCurrent + index

        return if (excluded.size >= total) base.withCurrent(false) else base.copy(excludedCurrent = excluded)
    }

    fun withIncomingLine(index: Int, keep: Boolean, total: Int): ConflictChoice {
        val base = if (keepIncoming) this else withIncoming(true).copy(excludedIncoming = (0 until total).toSet())
        val excluded = if (keep) base.excludedIncoming - index else base.excludedIncoming + index

        return if (excluded.size >= total) base.withIncoming(false) else base.copy(excludedIncoming = excluded)
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

    /**
     * Text of the resolved file, to be edited by hand. Conflicts without a choice keep no lines, like in [resolve].
     */
    fun resolvedText(choices: Map<Int, ConflictChoice>): String {
        val lines = chunks.flatMap { chunk ->
            when (chunk) {
                is ConflictChunk.Common -> chunk.lines.map { it.text }
                is ConflictChunk.Conflict -> chunk.linesFor(choices[chunk.index]).map { it.text }
            }
        }

        val resolvedBytes = resolve(choices)
        val endsWithNewLine = resolvedBytes.isNotEmpty() && resolvedBytes.last() == '\n'.code.toByte()

        return lines.joinToString("\n") + if (endsWithNewLine) "\n" else ""
    }

    /**
     * Indexes of the lines of a result [text] that are not common to both sides: lines kept from the conflicting blocks
     * and lines edited by hand. Lines are matched with a diff, so editing the text keeps the highlight on the right lines.
     */
    fun changedLines(text: String): Set<Int> {
        val commonText = chunks
            .filterIsInstance<ConflictChunk.Common>()
            .flatMap { it.lines }
            .joinToString("") { "${it.text}\n" }

        // The trailing new line of the text isn't a line of its own
        val resultText = text.removeSuffix("\n").lines().joinToString("") { "$it\n" }

        val edits = HistogramDiff().diff(
            RawTextComparator.DEFAULT,
            RawText(commonText.toByteArray()),
            RawText(resultText.toByteArray()),
        )

        return edits.flatMap { it.beginB until it.endB }.toSet()
    }

    /**
     * Encodes a text edited by hand like the original file: same line endings and, if it was valid UTF-8, same charset.
     */
    fun encode(text: String): ByteArray {
        val originalBytes = chunks.flatMap { chunk ->
            when (chunk) {
                is ConflictChunk.Common -> chunk.lines
                is ConflictChunk.Conflict -> chunk.current + chunk.incoming
            }
        }.fold(ByteArrayOutputStream()) { output, line -> output.apply { write(line.bytes) } }.toByteArray()

        val usesCrlf = String(originalBytes, Charsets.ISO_8859_1).contains("\r\n")
        val lfText = text.replace("\r\n", "\n")
        val textWithLineEndings = if (usesCrlf) lfText.replace("\n", "\r\n") else lfText

        // Lines that are not valid UTF-8 are decoded as ISO-8859-1, which keeps every byte as it was
        val charset = if (isValidUtf8(originalBytes)) Charsets.UTF_8 else Charsets.ISO_8859_1

        return textWithLineEndings.toByteArray(charset)
    }

    private fun isValidUtf8(bytes: ByteArray) = try {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
        true
    } catch (ex: CharacterCodingException) {
        false
    }

    companion object {
        /** Detects conflict markers typed or pasted by hand, which would leave the file broken */
        fun hasConflictMarkers(text: String) = text.lines().any { it.startsWith("<<<<<<<") || it.startsWith(">>>>>>>") }
    }
}
