package tribixbite.cleverkeys

/** Only the Manager's existing first canonical per normalized key; no prediction index. */
internal class DictionaryDisplayRows(private val disabled: Set<String>) {
    private val rows = LinkedHashMap<String, DictionaryWord>()

    fun add(canonical: String, rank: Int) {
        // Equivalent to AccentNormalizer for ASCII, without NFD/replacement passes.
        val key = if (canonical.all { it.code < 128 }) canonical.lowercase()
            else AccentNormalizer.normalize(canonical)
        if (key.isEmpty() || rows.containsKey(key)) return
        rows[key] = DictionaryWord(canonical, (10000 - rank * 39).coerceIn(1, 10000),
            WordSource.MAIN, key !in disabled && canonical !in disabled)
    }

    fun words(): List<DictionaryWord> = rows.values.toList()
}
