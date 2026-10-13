package tribixbite.cleverkeys

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

/** One load per language. Cancelling a view's await never cancels the shared load. */
internal class DictionaryDisplayLoadCache<T : Any>(dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    private class Entry<T> {
        lateinit var task: Deferred<T>
        var ready: T? = null
    }
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val lock = Any()
    private val entries = mutableMapOf<String, Entry<T>>()

    fun peek(language: String): T? = synchronized(lock) { entries[language]?.ready }

    suspend fun get(language: String, load: suspend () -> T): T {
        val entry = synchronized(lock) {
            entries[language] ?: Entry<T>().also { fresh ->
                fresh.task = scope.async(start = CoroutineStart.LAZY) {
                    val value = load()
                    synchronized(lock) {
                        if (entries[language] === fresh) fresh.ready = value
                    }
                    value
                }
                entries[language] = fresh
                fresh.task.invokeOnCompletion { cause ->
                    if (cause != null) synchronized(lock) {
                        if (entries[language] === fresh) entries.remove(language)
                    }
                }
            }
        }
        entry.task.start()
        return entry.task.await()
    }

    /** Retire before cancellation so a late old completion cannot evict a replacement. */
    fun clear(language: String? = null) {
        val retired = synchronized(lock) {
            if (language == null) entries.values.toList().also { entries.clear() }
            else listOfNotNull(entries.remove(language))
        }
        retired.forEach { it.task.cancel() }
    }
}
