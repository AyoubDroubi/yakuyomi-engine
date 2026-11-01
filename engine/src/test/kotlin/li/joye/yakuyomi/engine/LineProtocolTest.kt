package li.joye.yakuyomi.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class LineProtocolTest {

    @Test fun numbersLinesAndFlattensNewlines() {
        assertEquals("<|1|>A\n<|2|>B C", LineProtocol.numbered(listOf("A", "B\nC")))
    }

    @Test fun parsesTheAnswerAsTheTwoTestModelsReturnedIt() {
        // Index-Translate-2B on a phone answered exactly like this (one line per number, Arabic text).
        val raw = "<|1|>ما رأيك يا ويلسون؟\n<|2|>هل تود تذوقه أيضًا؟\n<|3|>سيكون ذلك\n<|4|>هدرًا لو أكلته بمفردك!"
        val parsed = LineProtocol.parse(raw)
        assertEquals(4, parsed.size)
        assertEquals("سيكون ذلك", parsed[3])
    }

    @Test fun dropsThinkingAndToleratesSloppyMarkers() {
        val parsed = LineProtocol.parse("<think>plan</think>\n<|1>first\n<2|>second\nnoise")
        assertEquals(mapOf(1 to "first"), parsed.filterKeys { it == 1 })
        assertEquals(null, parsed[3])
    }

    @Test fun missingLinesAreAbsentOrEmptyWithWords() {
        val queries = listOf("HELLO THERE", "...", "WAIT FOR ME")
        val parsed = mapOf(1 to "مرحبا", 2 to "")
        assertEquals(listOf(2), LineProtocol.missing(queries, parsed))
    }
}
