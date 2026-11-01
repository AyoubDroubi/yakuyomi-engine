package li.joye.yakuyomi.engine

import org.junit.Assert.assertEquals
import org.junit.Test

/** How the OCR lines of one bubble are joined before translation. */
class JoinOcrLinesTest {

    @Test fun englishLinesGetSpaces() {
        assertEquals(
            "IT WOULD BE A WASTE TO EAT IT ALONE!",
            joinOcrLines(listOf("IT WOULD BE A", "WASTE TO EAT IT", "ALONE!")),
        )
    }

    @Test fun japaneseLinesAreConcatenated() {
        assertEquals("もう一度ゲートが開いたって？", joinOcrLines(listOf("もう一度", "ゲートが開いたって？")))
    }

    @Test fun chineseAndFullWidthPunctuationStayTight() {
        assertEquals("你說什麼！？我不懂", joinOcrLines(listOf("你說什麼！？", "我不懂")))
    }

    @Test fun koreanLinesGetSpaces() {
        assertEquals("같이 먹을래?", joinOcrLines(listOf("같이", "먹을래?")))
    }

    @Test fun hyphenatedWordIsNotSplit() {
        assertEquals("A WA-STE OF TIME", joinOcrLines(listOf("A WA-", "STE OF TIME")))
    }

    @Test fun blankAndPaddedLinesAreIgnored() {
        assertEquals("WHAT DO YOU THINK, WILSON?", joinOcrLines(listOf(" WHAT ", "", "DO YOU THINK,", "WILSON?")))
    }

    @Test fun latinNextToKanaHasNoSpace() {
        assertEquals("OKです", joinOcrLines(listOf("OK", "です")))
    }
}
