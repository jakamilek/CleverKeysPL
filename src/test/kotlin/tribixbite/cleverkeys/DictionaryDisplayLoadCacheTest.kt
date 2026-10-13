package tribixbite.cleverkeys

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class DictionaryDisplayLoadCacheTest {
    @Test fun cancelledViewAndManyReplacementViewsShareOneCompleteLoad() = runBlocking {
        withTimeout(5000) {
            val cache = DictionaryDisplayLoadCache<Any>()
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val loads = AtomicInteger()
            val snapshot = Any()
            val first = async { cache.get("pl") {
                loads.incrementAndGet(); entered.complete(Unit); release.await(); snapshot
            } }
            entered.await()
            assertNull(cache.peek("pl"))
            first.cancelAndJoin()
            val replacements = (1..20).map { async(start = CoroutineStart.UNDISPATCHED) {
                cache.get("pl") { error("Duplicate load after tab switch") }
            } }
            release.complete(Unit)
            replacements.forEach { assertSame(snapshot, it.await()) }
            assertEquals(1, loads.get())
            assertSame(snapshot, cache.peek("pl"))
            cache.clear()
        }
    }

    @Test fun failureRetiresTaskAndAllowsRetry() = runBlocking {
        withTimeout(5000) {
            val cache = DictionaryDisplayLoadCache<String>()
            val fail = runCatching { cache.get("pl") { throw java.io.IOException("test failure") } }
            assertTrue(fail.exceptionOrNull() is java.io.IOException)
            assertNull(cache.peek("pl"))
            assertEquals("reloaded", cache.get("pl") { "reloaded" })
            cache.clear()
        }
    }

    @Test fun lateCancelledCompletionCannotPublishOrEvictReplacement() = runBlocking {
        withTimeout(5000) {
            val cache = DictionaryDisplayLoadCache<String>()
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val first = async { cache.get("pl") {
                withContext(NonCancellable) {
                    entered.complete(Unit); release.await(); "obsolete"
                }
            } }
            entered.await()
            cache.clear("pl")
            assertEquals("replacement", cache.get("pl") { "replacement" })
            release.complete(Unit)
            first.join()
            assertTrue(first.isCancelled)
            assertEquals("replacement", cache.peek("pl"))
            assertEquals("replacement", cache.get("pl") { error("Lost replacement") })
            cache.clear()
        }
    }

    @Test fun languageInvalidationIsScopedAndClearAllRetiresBoth() = runBlocking {
        withTimeout(5000) {
            val cache = DictionaryDisplayLoadCache<String>()
            assertEquals("pl1", cache.get("pl") { "pl1" })
            assertEquals("en1", cache.get("en") { "en1" })
            cache.clear("pl")
            assertNull(cache.peek("pl"))
            assertEquals("en1", cache.peek("en"))
            assertEquals("pl2", cache.get("pl") { "pl2" })
            cache.clear()
            assertNull(cache.peek("pl")); assertNull(cache.peek("en"))
        }
    }

    @Test fun allocationFailureCanBeRetriedWithoutKeepingTheFailedTask() = runBlocking {
        withTimeout(5000) {
            val cache = DictionaryDisplayLoadCache<String>()
            val fail = runCatching { cache.get("pl") { throw OutOfMemoryError("synthetic") } }
            assertTrue(fail.exceptionOrNull() is OutOfMemoryError)
            assertNull(cache.peek("pl"))
            assertEquals("ok", cache.get("pl") { "ok" })
            cache.clear()
        }
    }
}
