package tribixbite.cleverkeys.ai

import java.util.Locale
import tribixbite.cleverkeys.SwipeSurfaceVariants

/** Two decoded Polish keys at most; declared case variants or unchanged decoded surfaces. */
internal class HerbertFormGroup(surfaces: List<String>, sharedSourceLemma: Boolean = false) {
    val surfaces: List<String> = java.util.Collections.unmodifiableList(ArrayList(surfaces))

    init {
        require(this.surfaces.size in 2..SwipeSurfaceVariants.MAX_GROUP_SURFACES)
        require(this.surfaces.distinct().size == this.surfaces.size)
        require(this.surfaces.all { it.isNotBlank() && it.length <= 96 })
        val keys = this.surfaces.groupBy { it.lowercase(Locale.ROOT) }
        require(keys.size in 1..2 && keys.values.all { it.size <= 2 })
        require(keys.keys.map(SwipeSurfaceVariants::foldPolish).distinct().size == 1 || sharedSourceLemma)
        require(this.surfaces.all {
            val key = it.lowercase(Locale.ROOT)
            it == key || it == key.replaceFirstChar { first -> first.titlecase(Locale.ROOT) }
        })
    }

    /** Finite mean log probabilities from the original WWM contract; ties remain stable. */
    fun ordered(scores: Map<String, Float>, captured: HerbertRequestIdentity,
                current: HerbertRequestIdentity, now: Long, deadline: Long): List<String>? {
        if (captured != current || captured.language != "pl" ||
            captured.selectionStart != captured.selectionEnd || captured.context.isBlank() ||
            now >= deadline || scores.keys != surfaces.toSet() ||
            scores.values.any { !it.isFinite() }) return null
        return surfaces.sortedWith(compareByDescending<String> { scores.getValue(it) })
    }
}
