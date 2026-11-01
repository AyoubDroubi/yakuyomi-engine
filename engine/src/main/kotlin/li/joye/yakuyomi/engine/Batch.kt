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
    /** Detection text mask, one byte per pixel (0..255), [width]×[height] row-major. */
    internal val mask: ByteArray,
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
    val approxBytes: Long get() = mask.size.toLong()
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
    /** Token usage of the whole request is reported on the first page of the batch only. */
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
)

internal object MaskPacking {
    /** ARGB mask bitmap → one byte per pixel (blue channel, the one the inpainter reads). */
    fun pack(mask: Bitmap): ByteArray {
        val w = mask.width
        val h = mask.height
        val px = IntArray(w * h)
        mask.getPixels(px, 0, w, 0, 0, w, h)
        return packPixels(px)
    }

    fun packPixels(px: IntArray): ByteArray = ByteArray(px.size) { (px[it] and 0xFF).toByte() }

    fun unpackPixels(bytes: ByteArray): IntArray = IntArray(bytes.size) {
        val v = bytes[it].toInt() and 0xFF
        (0xFF shl 24) or (v shl 16) or (v shl 8) or v
    }

    fun unpack(bytes: ByteArray, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(unpackPixels(bytes), 0, w, 0, 0, w, h)
        return bmp
    }
}
