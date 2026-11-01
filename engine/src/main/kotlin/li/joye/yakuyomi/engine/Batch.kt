package li.joye.yakuyomi.engine

import android.graphics.Bitmap

/**
 * Multi-page translation (one LLM request for several pages), in three steps so the caller never has to keep
 * several full page bitmaps in memory while a batch is being translated:
 *
 *  1. [TranslationEngine.prepare]: detection + OCR + grouping on one page. The result keeps only the text regions and
 *     the detection mask packed into one byte per pixel; the page bitmap can be released right after.
 *  2. [TranslationEngine.translateBatch]: the OCR text of several prepared pages in a single request.
 *  3. [TranslationEngine.complete]: with the page decoded again, filter the translations, inpaint only the regions
 *     that got a translation (failed ones keep their artwork without any restore step) and typeset.
 */

/** A page after detection and OCR, waiting for its translation. Holds no bitmap. */
class PreparedPage internal constructor(
    val width: Int,
    val height: Int,
    /** Regions with OCR text, in reading order; their [TextRegion.sourceText] are the lines to translate. */
    val regions: List<TextRegion>,
    /** Detection text mask (binary), one bit per pixel, [width]×[height] row-major (see [MaskPacking]). */
    internal val mask: LongArray,
    internal val lineCount: Int,
    internal val regionCount: Int,
    internal val detectMs: Long,
    internal val ocrMs: Long,
) {
    /** The lines to translate for this page. */
    val queries: List<String> get() = regions.map { it.sourceText }

    /** Characters of OCR text on the page (used to size batches). */
    val textLength: Int get() = regions.sumOf { it.sourceText.length }

    /** Memory held by this object, roughly (the mask dominates). */
    val approxBytes: Long get() = mask.size.toLong() * 8
}

sealed interface PrepareResult {
    /** The page has text to translate. */
    data class Ready(val page: PreparedPage) : PrepareResult

    /** Nothing to translate (no text: [PageResult.Skipped]) or detection/OCR failed ([PageResult.Failed]). */
    data class Done(val result: PageResult) : PrepareResult
}

/** One page's share of a [TranslationEngine.translateBatch] call. */
data class PageTranslation(
    /** One entry per query; a line the model did not answer falls back to its source text (filtered out later). */
    val translations: List<String>,
    /** Non-null when the request failed or some of this page's lines were missing from the answer. */
    val error: String? = null,
    val raw: String? = null,
    /** Token usage: 0 for batch pages (the batch reports it once, [BatchTranslation]); set for single-page calls. */
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val translateMs: Long = 0,
)

/** Result of one [TranslationEngine.translateBatch] call. */
data class BatchTranslation(
    val pages: List<PageTranslation>,
    /** HTTP requests the batch took (1, or 2 when missing lines were asked again). */
    val requests: Int,
    /** True when the first answer missed lines (the caller can shrink later batches). */
    val hadMissingLines: Boolean,
    /** True when the request itself failed (network, timeout, rate limit): every page carries the error. */
    val failed: Boolean = false,
    /** Token usage of the whole batch (all its requests). */
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
)

/**
 * The detection text mask is binary (the detector writes only white or black), so prepared pages keep it as one bit
 * per pixel: a 10 MP page costs 1.25 MB instead of 40 MB as a bitmap.
 */
internal object MaskPacking {
    /** ARGB mask bitmap → bits (set = text; blue channel > 127, the test the inpainter uses). */
    fun pack(mask: Bitmap): LongArray {
        val w = mask.width
        val h = mask.height
        val out = LongArray((w * h + 63) / 64)
        val row = IntArray(w)
        for (y in 0 until h) {
            mask.getPixels(row, 0, w, 0, y, w, 1)
            val base = y * w
            for (x in 0 until w) if ((row[x] and 0xFF) > 127) setBit(out, base + x)
        }
        return out
    }

    fun packPixels(px: IntArray): LongArray {
        val out = LongArray((px.size + 63) / 64)
        for (i in px.indices) if ((px[i] and 0xFF) > 127) setBit(out, i)
        return out
    }

    fun unpackPixels(bits: LongArray, size: Int): IntArray = IntArray(size) {
        if ((bits[it ushr 6] ushr (it and 63)) and 1L != 0L) WHITE else BLACK
    }

    fun unpack(bits: LongArray, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val row = IntArray(w)
        for (y in 0 until h) {
            val base = y * w
            for (x in 0 until w) {
                val i = base + x
                row[x] = if ((bits[i ushr 6] ushr (i and 63)) and 1L != 0L) WHITE else BLACK
            }
            bmp.setPixels(row, 0, w, 0, y, w, 1)
        }
        return bmp
    }

    private fun setBit(bits: LongArray, i: Int) {
        bits[i ushr 6] = bits[i ushr 6] or (1L shl (i and 63))
    }

    private const val WHITE = -0x1 // 0xFFFFFFFF
    private const val BLACK = -0x1000000 // 0xFF000000
}
