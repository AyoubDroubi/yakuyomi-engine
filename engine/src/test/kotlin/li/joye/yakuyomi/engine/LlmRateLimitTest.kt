package li.joye.yakuyomi.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** How long to wait after an HTTP 429/503, read from the provider's response. */
class LlmRateLimitTest {

    @Test fun retryAfterHeaderWins() {
        assertEquals(7_000L, LlmTranslator.parseRetryAfterMs("7", """{"retryDelay":"30s"}"""))
    }

    @Test fun geminiRetryDelayInBody() {
        val body = """{"error":{"code":429,"details":[{"@type":"type.googleapis.com/google.rpc.RetryInfo","retryDelay": "23s"}]}}"""
        assertEquals(23_000L, LlmTranslator.parseRetryAfterMs(null, body))
    }

    @Test fun fractionalSeconds() {
        assertEquals(1_500L, LlmTranslator.parseRetryAfterMs(null, """{"retryDelay":"1.5s"}"""))
    }

    @Test fun unknownWhenAbsent() {
        assertNull(LlmTranslator.parseRetryAfterMs(null, """{"error":{"code":429,"message":"slow down"}}"""))
        assertNull(LlmTranslator.parseRetryAfterMs("Wed, 21 Oct 2026 07:28:00 GMT", "{}"))
    }
}
