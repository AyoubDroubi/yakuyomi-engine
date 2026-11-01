package li.joye.yakuyomi.engine

import org.junit.Assert.assertEquals
import org.junit.Test

/** Untranslated bubbles get their original pixels back; translated neighbours stay inpainted. */
class OriginalRestoreTest {

    private val w = 6
    private val h = 4
    private fun original() = IntArray(w * h) { 1 }
    private fun cleaned() = IntArray(w * h) { 0 }

    @Test fun failedBoxIsRestored() {
        val dst = cleaned()
        OriginalRestore.copyBack(dst, original(), w, h, listOf(PixelRect(0, 0, 2, 2)), emptyList())
        assertEquals(4, dst.count { it == 1 })
        assertEquals(1, dst[0]); assertEquals(1, dst[1 * w + 1]); assertEquals(0, dst[1 * w + 2])
    }

    @Test fun overlapWithTranslatedBoxStaysCleaned() {
        val dst = cleaned()
        OriginalRestore.copyBack(dst, original(), w, h, listOf(PixelRect(0, 0, 4, 1)), listOf(PixelRect(2, 0, 6, 4)))
        assertEquals(listOf(1, 1, 0, 0, 0, 0), dst.take(w))
    }

    @Test fun boxesOutsideThePageAreClamped() {
        val dst = cleaned()
        OriginalRestore.copyBack(dst, original(), w, h, listOf(PixelRect(-5, 2, 99, 99)), emptyList())
        assertEquals(2 * w, dst.count { it == 1 })
    }
}
