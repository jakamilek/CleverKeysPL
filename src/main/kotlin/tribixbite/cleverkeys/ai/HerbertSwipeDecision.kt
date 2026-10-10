package tribixbite.cleverkeys.ai

import tribixbite.cleverkeys.SwipeSurfaceVariants

/** Main-thread, one-shot decision. Timeout/next touch uses the original slate; stale edits use none. */
internal class HerbertSwipeDecision(
    private val slate: SwipeSurfaceVariants.Slate,
    private val group: HerbertFormGroup,
    private val captured: HerbertRequestIdentity,
    private val deadline: Long,
) {
    constructor(slate: SwipeSurfaceVariants.Slate, pair: HerbertCasePair,
                captured: HerbertRequestIdentity, deadline: Long) :
        this(slate, HerbertFormGroup(pair.surfaces), captured, deadline)
    private var finished = false
    fun cancel() { finished = true }
    fun finish(scores: Map<String, Float>?, current: HerbertRequestIdentity?, now: Long): SwipeSurfaceVariants.Slate? {
        if (finished) return null
        finished = true
        if (current != captured) return null
        val order = scores?.let { group.ordered(it, captured, captured, now, deadline) }
        return if (order == null) slate else HerbertLiveSlate.ordered(slate, order)
    }
}
