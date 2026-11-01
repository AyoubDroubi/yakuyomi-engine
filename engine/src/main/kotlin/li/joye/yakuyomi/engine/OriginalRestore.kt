package li.joye.yakuyomi.engine

import android.graphics.Bitmap

/** Integer pixel rectangle, [x0, x1) × [y0, y1). */
internal data class PixelRect(val x0: Int, val y0: Int, val x1: Int, val y1: Int) {
    fun contains(x: Int, y: Int) = x in x0 until x1 && y in y0 until y1
}

/** The region's bounding box grown by [margin] px on every side. */
internal fun TextRegion.rect(margin: Int) = PixelRect(
    (x0 - margin).toInt(),
    (y0 - margin).toInt(),
    kotlin.math.ceil(x1 + margin).toInt(),
    kotlin.math.ceil(y1 + margin).toInt(),
)

/**
 * Puts the original artwork back over regions whose translation failed. Inpainting runs on every text region in
 * parallel with translation (before it is known which ones will fail) and only touches pixels inside each region's
 * box grown by the mask padding and dilation, so copying those boxes back from the original undoes it. Pixels that
 * also fall inside a translated region's box are left alone, so a neighbouring translated bubble stays clean.
 */
internal object OriginalRestore {

    fun restore(cleaned: Bitmap, original: Bitmap, restore: List<PixelRect>, protect: List<PixelRect>) {
        if (restore.isEmpty()) return
        val w = cleaned.width
        val h = cleaned.height
        if (original.width != w || original.height != h || !cleaned.isMutable) return
        val dst = IntArray(w * h).also { cleaned.getPixels(it, 0, w, 0, 0, w, h) }
        val src = IntArray(w * h).also { original.getPixels(it, 0, w, 0, 0, w, h) }
        copyBack(dst, src, w, h, restore, protect)
        cleaned.setPixels(dst, 0, w, 0, 0, w, h)
    }

    /** Pure pixel step of [restore] (row-major ARGB arrays of [w]×[h]); separate so it can be unit-tested on the JVM. */
    fun copyBack(dst: IntArray, src: IntArray, w: Int, h: Int, restore: List<PixelRect>, protect: List<PixelRect>) {
        for (r in restore) {
            val x0 = r.x0.coerceIn(0, w)
            val x1 = r.x1.coerceIn(0, w)
            val y0 = r.y0.coerceIn(0, h)
            val y1 = r.y1.coerceIn(0, h)
            for (y in y0 until y1) {
                val row = y * w
                for (x in x0 until x1) {
                    if (protect.any { it.contains(x, y) }) continue
                    dst[row + x] = src[row + x]
                }
            }
        }
    }
}
