package li.joye.yakuyomi.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Line breaking and RTL detection; measured with "1 char = 1 unit" so no Android Paint is needed. */
class RendererWrapTest {

    private val len: (String) -> Float = { it.length.toFloat() }

    @Test fun rtlDetection() {
        assertTrue(Renderer.isRtl("ماذا تفعل؟"))
        assertTrue(Renderer.isRtl("!!! لا"))
        assertTrue(Renderer.isRtl("שלום"))
        assertFalse(Renderer.isRtl("Hello!"))
        assertFalse(Renderer.isRtl("何をしている"))
    }

    @Test fun arabicBreaksOnlyAtSpaces() {
        val lines = Renderer.wrapText("ماذا تفعل هنا يا رجل؟ لا أصدق هذا!", 12f, len)
        assertEquals(listOf("ماذا تفعل", "هنا يا رجل؟", "لا أصدق هذا!"), lines)
        // every original word survives intact
        assertEquals("ماذا تفعل هنا يا رجل؟ لا أصدق هذا!", lines.joinToString(" "))
    }

    @Test fun latinBreaksOnlyAtSpaces() {
        assertEquals(
            listOf("Hello", "there,", "what are", "you doing", "here?"),
            Renderer.wrapText("Hello there, what are you doing here?", 10f, len),
        )
    }

    @Test fun overlongWordFallsBackToCharacterSplit() {
        assertEquals(listOf("سووووو", "وووووو", "وووووو"), Renderer.wrapText("سووووووووووووووووو", 6f, len))
    }

    @Test fun explicitNewlinesAreKept() {
        assertEquals(listOf("سطر أول", "سطر ثاني"), Renderer.wrapText("سطر أول\nسطر ثاني", 20f, len))
    }

    @Test fun cjkKeepsPerCharacterBreakingWithKinsoku() {
        // "？" may not start a line, so it stays on the previous one.
        assertEquals(listOf("何をして", "いるんだ？"), Renderer.wrapText("何をしているんだ？", 4f, len))
    }
}
