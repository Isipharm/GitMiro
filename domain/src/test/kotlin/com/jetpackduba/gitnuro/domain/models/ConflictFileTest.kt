package com.jetpackduba.gitnuro.domain.models

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
}
