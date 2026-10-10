package tribixbite.cleverkeys.ai

import java.util.Locale
import tribixbite.cleverkeys.SwipeSurfaceVariants
import tribixbite.cleverkeys.langpack.LanguageIntelligenceProvider

/** Only a bounded source group moves; each word travels with all its parallel metadata. */
internal object HerbertLiveSlate {
    fun group(slate: SwipeSurfaceVariants.Slate, provider: LanguageIntelligenceProvider? = null): HerbertFormGroup? {
        val size = slate.words.size
        if (slate.formGroupSize < 0) return null
        val count = if (slate.formGroupSize > 0) slate.formGroupSize else slate.exactCase.takeWhile { it }.size
        if (count !in 2..SwipeSurfaceVariants.MAX_GROUP_SURFACES || count > size ||
            slate.exactCase.size != size || slate.exactCase.drop(count).any { it } ||
            slate.scores.size != size ||
            slate.languages?.let { it.size != size || it.take(count).any { lang -> lang != "pl" } } == true) return null
        val indices = (0 until count).groupBy { slate.words[it].lowercase(Locale.ROOT) }
        if (indices.values.any { positions -> positions.map { slate.scores[it] }.distinct().size != 1 }) return null
        val keys = indices.keys.toList()
        val shared = keys.size == 2 && provider?.sharesSourceLemma(keys[0], keys[1]) == true
        return try { HerbertFormGroup(slate.words.take(count), shared) }
        catch (_: IllegalArgumentException) { null }
    }

    fun pair(slate: SwipeSurfaceVariants.Slate): HerbertCasePair? {
        if (slate.words.size < 2 || slate.exactCase.count { it } != 2 ||
            !slate.exactCase.take(2).all { it } || slate.scores.size != slate.words.size ||
            slate.scores[0] != slate.scores[1] ||
            slate.languages?.let { it.size != slate.words.size || it[0] != it[1] } == true) return null
        return try { HerbertCasePair(slate.words[0].lowercase(Locale.ROOT), slate.words.take(2)) }
        catch (_: IllegalArgumentException) { null }
    }

    fun ordered(slate: SwipeSurfaceVariants.Slate, surfaces: List<String>): SwipeSurfaceVariants.Slate {
        val count = surfaces.size
        require(count in 2..SwipeSurfaceVariants.MAX_GROUP_SURFACES &&
            surfaces.distinct().size == count && surfaces.toSet() == slate.words.take(count).toSet())
        val order = surfaces.map { slate.words.take(count).indexOf(it) } + (count until slate.words.size)
        return slate.copy(words = order.map { slate.words[it] }, scores = order.map { slate.scores[it] },
            languages = slate.languages?.let { languages -> order.map { languages[it] } },
            exactCase = order.map { slate.exactCase[it] })
    }
}
