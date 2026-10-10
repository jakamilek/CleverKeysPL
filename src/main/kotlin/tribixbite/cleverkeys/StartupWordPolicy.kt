package tribixbite.cleverkeys

/** A small, bounded top list. Personal counts take priority over lexical frequency. */
object StartupWordPolicy {
    fun select(
        personal: List<Pair<String, Int>>, lexicon: Map<String, Int>,
        maxResults: Int = 3, excludeFallback: Set<String> = emptySet(),
        allowed: (String) -> Boolean = { true }
    ): List<String> {
        val limit = maxResults.coerceIn(0, 10)
        if (limit == 0) return emptyList()
        fun eligible(word: String) = word.isNotEmpty() && word.all { it.isLetter() } && allowed(word)
        val selected = personal.filter { it.second > 0 && eligible(it.first) }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
            .map { it.first }.distinct().take(limit).toMutableList()
        if (selected.size == limit) return selected
        val remaining = limit - selected.size
        val best = mutableListOf<Pair<String, Int>>()
        val order = compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first }
        for ((word, frequency) in lexicon) {
            if (frequency <= 0 || word in selected || word in excludeFallback || !eligible(word)) continue
            best.add(word to frequency)
            best.sortWith(order)
            if (best.size > remaining) best.removeAt(best.lastIndex)
        }
        return selected + best.map { it.first }
    }
}
