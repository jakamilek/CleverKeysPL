package tribixbite.cleverkeys.ai

import java.util.Locale
import tribixbite.cleverkeys.SwipeSurfaceVariants

/** No additions, removed keys, score mixing or language changes: only the first source pair moves. */
internal object HerbertLiveSlate {
    fun pair(slate: SwipeSurfaceVariants.Slate): HerbertCasePair? {
        if (slate.words.size < 2 || slate.exactCase.count { it } != 2 ||
            !slate.exactCase.take(2).all { it } || slate.scores.size != slate.words.size ||
            slate.scores[0] != slate.scores[1] ||
            slate.languages?.let { it.size != slate.words.size || it[0] != it[1] } == true) return null
        return try { HerbertCasePair(slate.words[0].lowercase(Locale.ROOT), slate.words.take(2)) }
        catch (_: IllegalArgumentException) { null }
    }

    fun ordered(slate: SwipeSurfaceVariants.Slate, surfaces: List<String>): SwipeSurfaceVariants.Slate {
        require(surfaces.size == 2 && surfaces.toSet() == slate.words.take(2).toSet())
        return slate.copy(words = surfaces + slate.words.drop(2))
    }
}
