package li.joye.yakuyomi.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/** Prepared pages keep the detection mask as one byte per pixel; the inpainter reads the blue channel back. */
class MaskPackingTest {

    @Test fun roundTripKeepsTheChannelTheInpainterReads() {
        val px = intArrayOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF808080.toInt(), 0xFF0000C8.toInt())
        val bytes = MaskPacking.packPixels(px)
        assertEquals(4, bytes.size)
        val back = MaskPacking.unpackPixels(bytes)
        assertArrayEquals(px.map { it and 0xFF }.toIntArray(), back.map { it and 0xFF }.toIntArray())
        back.forEach { assertEquals(0xFF, it ushr 24) } // opaque
    }
}
