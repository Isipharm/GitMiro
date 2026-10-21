package com.hugodehu.gitmiro.domain.models

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ConflictFileTest {
    private fun line(text: String) = ConflictLine(text.removeSuffix("\n"), text.toByteArray())

    private val file = ConflictFile(
        listOf(
            ConflictChunk.Common(listOf(line("a\n"))),
            ConflictChunk.Conflict(0, current = listOf(line("current 1\n")), incoming = listOf(line("incoming 1\n"))),
            ConflictChunk.Common(listOf(line("b\n"))),
            ConflictChunk.Conflict(1, current = listOf(line("current 2")), incoming = listOf(line("incoming 2"))),
        )
    )

    @Test
    fun `keeps the chosen sides, current first when both are kept`() {
        val choices = mapOf(
            0 to ConflictChoice(keepIncoming = true),
            1 to ConflictChoice(keepCurrent = true, keepIncoming = true),
        )

        // "current 2" has no line ending (end of file), one is added because "incoming 2" follows
        assertEquals("a\nincoming 1\nb\ncurrent 2\nincoming 2", String(file.resolve(choices)))
        assertTrue(file.isFullyDecided(choices))
    }

    @Test
    fun `both sides follow the order in which they were kept`() {
        val incomingThenCurrent = ConflictChoice().withIncoming(true).withCurrent(true)
        val currentThenIncoming = ConflictChoice().withCurrent(true).withIncoming(true)

        assertEquals(
            "a\nincoming 1\ncurrent 1\nb\ncurrent 2\nincoming 2",
            String(file.resolve(mapOf(0 to incomingThenCurrent, 1 to currentThenIncoming))),
        )

        // Keeping again a side that is already kept (e.g. with the "keep all" checkbox) keeps the order
        assertEquals(incomingThenCurrent, incomingThenCurrent.withIncoming(true).withCurrent(true))
        // Removing then adding again a side puts it last
        assertFalse(incomingThenCurrent.withIncoming(false).withIncoming(true).incomingFirst)
    }

    @Test
    fun `not fully decided while a conflict has no side kept`() {
        assertFalse(file.isFullyDecided(mapOf(0 to ConflictChoice(keepCurrent = true))))
        assertFalse(file.isFullyDecided(mapOf(0 to ConflictChoice(keepCurrent = true), 1 to ConflictChoice())))
    }

    @Test
    fun `text for manual edition matches the resolved content`() {
        val choices = mapOf(0 to ConflictChoice(keepCurrent = true), 1 to ConflictChoice(keepIncoming = true))

        assertEquals("a\ncurrent 1\nb\nincoming 2", file.resolvedText(choices))
        assertEquals(String(file.resolve(choices)), file.resolvedText(choices))
    }

    @Test
    fun `conflict markers typed by hand are detected`() {
        assertTrue(ConflictFile.hasConflictMarkers("a\n<<<<<<< HEAD\nb\n=======\nc\n>>>>>>> other\n"))
        assertFalse(ConflictFile.hasConflictMarkers("title\n=======\nb"))
    }

    @Test
    fun `edited text is encoded with the line endings and charset of the original file`() {
        fun latin1Line(text: String) = ConflictLine(text.trimEnd(), text.toByteArray(Charsets.ISO_8859_1))

        val crlfLatin1File = ConflictFile(
            listOf(
                ConflictChunk.Common(listOf(latin1Line("café\r\n"))),
                ConflictChunk.Conflict(0, current = listOf(latin1Line("x\r\n")), incoming = listOf(latin1Line("y\r\n"))),
            )
        )

        assertArrayEquals(
            "café\r\nedited\r\n".toByteArray(Charsets.ISO_8859_1),
            crlfLatin1File.encode("café\nedited\n"),
        )
        assertArrayEquals("a\nb\n".toByteArray(), file.encode("a\r\nb\n"))
    }
}
