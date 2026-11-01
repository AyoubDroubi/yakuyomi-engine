package li.joye.yakuyomi.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/** Prepared pages keep the binary detection mask as bits; the inpainter reads the blue channel back (> 127 = text). */
class MaskPackingTest {

    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()

    @Test fun roundTripOfABinaryMask() {
        val px = IntArray(130) { if (it % 3 == 0 || it == 129) white else black }
        val bits = MaskPacking.packPixels(px)
        assertEquals(3, bits.size) // 130 bits -> 3 longs
        assertArrayEquals(px, MaskPacking.unpackPixels(bits, px.size))
    }

    @Test fun thresholdMatchesTheInpainter() {
        val px = intArrayOf(0xFF808080.toInt(), 0xFF7F7F7F.toInt())
        assertArrayEquals(intArrayOf(white, black), MaskPacking.unpackPixels(MaskPacking.packPixels(px), 2))
    }
}
