package li.joye.yakuyomi.engine

import org.junit.Assert.assertEquals
import org.junit.Test

/** Untranslated bubbles get their original pixels back; pixels a translated bubble inpainted stay cleaned. */
class OriginalRestoreTest {

    private val w = 6
    private val h = 4
    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()
    private fun original() = IntArray(w * h) { 1 }
    private fun cleaned() = IntArray(w * h) { 0 }

    @Test fun failedBoxIsRestored() {
        val dst = cleaned()
        OriginalRestore.copyBack(dst, original(), w, h, listOf(PixelRect(0, 0, 2, 2)), null)
        assertEquals(4, dst.count { it == 1 })
        assertEquals(1, dst[0]); assertEquals(1, dst[1 * w + 1]); assertEquals(0, dst[1 * w + 2])
    }

    @Test fun translatedRegionMaskStaysCleaned() {
        val dst = cleaned()
        // The translated bubble only inpainted x = 3 on the first row; the rest of the failed box is restored.
        val protect = IntArray(w * h) { black }.also { it[3] = white }
        OriginalRestore.copyBack(dst, original(), w, h, listOf(PixelRect(0, 0, 6, 1)), protect)
        assertEquals(listOf(1, 1, 1, 0, 1, 1), dst.take(w))
    }

    @Test fun boxesOutsideThePageAreClamped() {
        val dst = cleaned()
        OriginalRestore.copyBack(dst, original(), w, h, listOf(PixelRect(-5, 2, 99, 99)), null)
        assertEquals(2 * w, dst.count { it == 1 })
    }
}
